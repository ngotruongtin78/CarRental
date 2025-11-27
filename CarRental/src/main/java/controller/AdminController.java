package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.UserRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.RentalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentalRecordService rentalRecordService;
    private final VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RentalRecordService rentalRecordService,
                           VehicleRepository vehicleRepository) {
        this.userRepository = userRepository;
        this.rentalRecordService = rentalRecordService;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/vehicles") public String showVehicleManagement() { return "admin-vehicles"; }
    @GetMapping("/stations") public String showStationManagement() { return "admin-stations"; }
    @GetMapping("/customers") public String showCustomerManagement() { return "admin-customers"; }
    @GetMapping("/staff") public String showStaffManagement() { return "admin-staff"; }
    @GetMapping("/history") public String showHistoryManagement() { return "admin-history"; }
    @GetMapping("/reports") public String showReportsDashboard() { return "admin-reports"; }
    @GetMapping("/vehicle-reports") public String showVehicleReportsManagement() { return "admin-vehicle-reports"; }
    @GetMapping("/ratings") public String showRatingsDashboard() { return "admin-ratings"; }

    @GetMapping("/staff/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllStaff() {
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (User user : allUsers) {
            try {
                if (user == null) continue;
                if ("admin".equalsIgnoreCase(user.getUsername())) continue;

                String role = user.getRole() != null ? user.getRole() : "USER";
                if (!"ROLE_STAFF".equals(role) && !"ROLE_ADMIN".equals(role)) continue;

                Map<String, Object> staffData = new LinkedHashMap<>();
                staffData.put("id", user.getId());
                staffData.put("fullName", user.getUsername());
                staffData.put("username", user.getUsername());
                staffData.put("role", role);
                staffData.put("status", user.isEnabled() ? "WORKING" : "RESIGNED");
                staffData.put("stationId", user.getStationId());
                staffData.put("performance", 0);
                responseList.add(staffData);
            } catch (Exception e) {}
        }
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/staff/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateStaff(@PathVariable("id") String id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Không tìm thấy nhân viên");

        String newPass = payload.get("password");
        if (newPass != null && !newPass.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPass));
        }

        user.setStationId(payload.get("stationId")); // Lưu trạm

        String role = payload.get("role");
        if (role != null && !role.isEmpty()) {
            user.setRole(role);
        }

        userRepository.save(user);
        return ResponseEntity.ok("Cập nhật thành công");
    }

    @GetMapping("/customers/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        List<User> allUsers = userRepository.findAll();
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (User user : allUsers) {
            try {
                if (user == null) continue;
                String role = user.getRole() != null ? user.getRole() : "USER";
                if ("ROLE_ADMIN".equals(role) || "ROLE_STAFF".equals(role)) continue;

                String username = user.getUsername() != null ? user.getUsername() : "Unknown";

                Map<String, Object> stats = rentalRecordService.calculateStats(username);

                Map<String, Object> customerData = new LinkedHashMap<>();
                customerData.put("id", user.getId());
                customerData.put("fullName", username);
                customerData.put("username", username);
                customerData.put("enabled", user.isEnabled());
                customerData.put("verified", user.isVerified());
                customerData.put("risk", user.isRisk());
                customerData.put("totalTrips", stats.getOrDefault("totalTrips", 0));
                customerData.put("totalSpent", stats.getOrDefault("totalSpent", 0));

                responseList.add(customerData);
            } catch (Exception e) {}
        }
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/customers/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleCustomerStatus(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(user.isEnabled() ? "ACTIVATED" : "DISABLED");
    }

    @PostMapping("/customers/toggle-risk/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleCustomerRisk(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");
        user.setRisk(!user.isRisk());
        userRepository.save(user);
        return ResponseEntity.ok(user.isRisk() ? "RISK_MARKED" : "RISK_REMOVED");
    }

    @GetMapping("/customers/view/{id}")
    public String showCustomerDetailPage(@PathVariable("id") String userId, Model model) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/admin/customers";
        Map<String, Object> stats = rentalRecordService.calculateStats(user.getUsername());
        List<Map<String, Object>> history = rentalRecordService.getHistoryDetails(user.getUsername());
        model.addAttribute("customer", user);
        model.addAttribute("stats", stats);
        model.addAttribute("history", history);
        return "admin-customer-detail";
    }

    @GetMapping("/reports/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportData() {
        Map<String, Object> stats = rentalRecordService.getGlobalStats();
        stats.put("aiSuggestions", rentalRecordService.getAiSuggestions());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cleanup-vehicles")
    @ResponseBody
    public ResponseEntity<String> cleanupDuplicateVehicles() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        Map<String, List<Vehicle>> grouped = allVehicles.stream()
                .filter(v -> v.getPlate() != null)
                .collect(Collectors.groupingBy(Vehicle::getPlate));

        int deletedCount = 0;
        for (Map.Entry<String, List<Vehicle>> entry : grouped.entrySet()) {
            List<Vehicle> group = entry.getValue();
            if (group.size() > 1) {
                group.sort((v1, v2) -> {
                    boolean s1 = v1.getBookingStatus() != null;
                    boolean s2 = v2.getBookingStatus() != null;
                    if (s1 && !s2) return -1;
                    if (!s1 && s2) return 1;
                    return v2.getId().compareTo(v1.getId());
                });
                for (int i = 1; i < group.size(); i++) {
                    vehicleRepository.delete(group.get(i));
                    deletedCount++;
                }
            }
        }
        return ResponseEntity.ok("Đã dọn dẹp xong. Tổng xe xóa: " + deletedCount);
    }
}
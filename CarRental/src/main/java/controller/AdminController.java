package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Staff;
import CarRental.example.document.User;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.StaffRepository;
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

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentalRecordService rentalRecordService;
    private final VehicleRepository vehicleRepository;

    @Autowired
    private RentalRecordRepository rentalRecordRepository;

    @Autowired
    private StaffRepository staffRepository;

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
    @GetMapping("/support") public String showSupportManagement() { return "admin-support"; }

    @GetMapping("/staff/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllStaff() {
        List<User> allUsers = userRepository.findAll();
        List<RentalRecord> allRentals = rentalRecordRepository.findAll();

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (User user : allUsers) {
            try {
                if (user == null) continue;
                if ("admin".equalsIgnoreCase(user.getUsername())) continue;

                String role = user.getRole() != null ? user.getRole() : "USER";
                if (!"ROLE_STAFF".equals(role) && !"ROLE_ADMIN".equals(role)) continue;

                long deliveryCount = allRentals.stream()
                        .filter(r -> r.getDeliveryStaffId() != null && r.getDeliveryStaffId().equals(user.getId()))
                        .count();

                long returnCount = allRentals.stream()
                        .filter(r -> r.getReturnStaffId() != null && r.getReturnStaffId().equals(user.getId()))
                        .count();

                long totalPerformance = deliveryCount + returnCount;

                Map<String, Object> staffData = new LinkedHashMap<>();
                staffData.put("id", user.getId());
                staffData.put("fullName", user.getUsername());
                staffData.put("username", user.getUsername());
                staffData.put("role", role);
                staffData.put("status", user.isEnabled() ? "WORKING" : "RESIGNED");
                staffData.put("stationId", user.getStationId());
                staffData.put("deliveryCount", deliveryCount);
                staffData.put("returnCount", returnCount);
                staffData.put("performance", totalPerformance);

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
        if (newPass != null && !newPass.trim().isEmpty()) user.setPassword(passwordEncoder.encode(newPass));
        user.setStationId(payload.get("stationId"));
        String role = payload.get("role");
        if (role != null && !role.isEmpty()) user.setRole(role);
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
        if (user != null) { user.setEnabled(!user.isEnabled()); userRepository.save(user); }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/customers/toggle-risk/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleCustomerRisk(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) { user.setRisk(!user.isRisk()); userRepository.save(user); }
        return ResponseEntity.ok("OK");
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
        return ResponseEntity.ok("OK");
    }

    // --- HÀM FIX DỮ LIỆU CŨ ---
    @GetMapping("/fix-staff-data")
    @ResponseBody
    public ResponseEntity<String> fixStaffData() {
        Staff targetStaff = staffRepository.findByUsername("staff");
        if (targetStaff == null) return ResponseEntity.badRequest().body("Không tìm thấy user 'staff'");

        String staffId = targetStaff.getId();
        List<RentalRecord> allRecords = rentalRecordRepository.findAll();
        int count = 0;
        for (RentalRecord r : allRecords) {
            boolean updated = false;
            String status = r.getStatus() != null ? r.getStatus() : "";

            if (status != null && !status.equals("PENDING") && !status.equals("CANCELLED") && !status.equals("EXPIRED")) {
                if (r.getDeliveryStaffId() == null) { r.setDeliveryStaffId(staffId); updated = true; }
            }
            if (status != null && (status.equals("COMPLETED") || status.equals("RETURNED"))) {
                if (r.getReturnStaffId() == null) { r.setReturnStaffId(staffId); updated = true; }
            }
            if (updated) { rentalRecordRepository.save(r); count++; }
        }
        return ResponseEntity.ok("Đã cập nhật dữ liệu cho " + count + " đơn hàng.");
    }
}
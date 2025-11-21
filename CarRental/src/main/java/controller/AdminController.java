package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import CarRental.example.service.RentalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentalRecordService rentalRecordService;

    public AdminController(UserRepository userRepository, RentalRecordService rentalRecordService) {
        this.userRepository = userRepository;
        this.rentalRecordService = rentalRecordService;
    }

    @GetMapping("/vehicles") public String showVehicleManagement() { return "admin-vehicles"; }
    @GetMapping("/stations") public String showStationManagement() { return "admin-stations"; }
    @GetMapping("/customers") public String showCustomerManagement() { return "admin-customers"; }
    @GetMapping("/staff") public String showStaffManagement() { return "admin-staff"; }
    @GetMapping("/history") public String showHistoryManagement() { return "admin-history"; }
    @GetMapping("/reports") public String showReportsDashboard() { return "admin-reports"; }

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
            } catch (Exception e) {
                System.err.println("Error user " + user.getId() + ": " + e.getMessage());
            }
        }
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/customers/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleCustomerStatus(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        boolean newStatus = !user.isEnabled();
        user.setEnabled(newStatus);
        userRepository.save(user);
        return ResponseEntity.ok(newStatus ? "ACTIVATED" : "DISABLED");
    }

    @PostMapping("/customers/toggle-risk/{id}")
    @ResponseBody
    public ResponseEntity<String> toggleCustomerRisk(@PathVariable("id") String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        boolean newRiskStatus = !user.isRisk();
        user.setRisk(newRiskStatus);
        userRepository.save(user);

        return ResponseEntity.ok(newRiskStatus ? "RISK_MARKED" : "RISK_REMOVED");
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
        return ResponseEntity.ok(stats);
    }
}
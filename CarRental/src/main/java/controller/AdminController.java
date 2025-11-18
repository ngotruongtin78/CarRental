package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import CarRental.example.service.RentalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RentalRecordService rentalRecordService;

    public AdminController(UserRepository userRepository, RentalRecordService rentalRecordService) {
        this.userRepository = userRepository;
        this.rentalRecordService = rentalRecordService;
    }

    @GetMapping("/vehicles")
    public String showVehicleManagement() {
        return "admin-vehicles";
    }
    @GetMapping("/stations")
    public String showStationManagement() {
        return "admin-stations";
    }
    @GetMapping("/customers")
    public String showCustomerManagement() { return "admin-customers"; }
    @GetMapping("/staff")
    public String showStaffManagement() { return "admin-staff"; }
    @GetMapping("/history")
    public String showHistoryManagement() {
        return "admin-history";
    }
    @GetMapping("/reports")
    public String showReportsDashboard() { return "admin-reports"; }

    @GetMapping("/customers/all")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        List<User> allUsers = userRepository.findAll();
        List<User> customers = allUsers.stream()
                .filter(user -> {
                    String role = user.getRole();
                    boolean isAdminRole = "ROLE_ADMIN".equals(role);
                    boolean isStaffRole = "ROLE_STAFF".equals(role);
                    return !(isAdminRole || isStaffRole);
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> responseList = new ArrayList<>();
        for (User user : customers) {
            Map<String, Object> stats = rentalRecordService.calculateStats(user.getUsername());
            Map<String, Object> customerData = new LinkedHashMap<>();
            customerData.put("id", user.getId());
            customerData.put("fullName", user.getUsername());
            customerData.put("enabled", user.isEnabled());
            customerData.put("verified", user.isVerified());
            customerData.put("totalTrips", stats.getOrDefault("totalTrips", 0));
            customerData.put("totalSpent", stats.getOrDefault("totalSpent", 0));
            responseList.add(customerData);
        }
        return ResponseEntity.ok(responseList);
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
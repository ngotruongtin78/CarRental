package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
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
}
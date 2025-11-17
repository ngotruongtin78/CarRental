package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {
    @GetMapping("/receive")
    public String showReceiveVehiclePage() {
        return "staff-return-vehicle";
    }
    @GetMapping("/deliver")
    public String showDeliverVehiclePage() {
        return "staff-deliver-vehicle";
    }
    @GetMapping("/verify")
    public String showVerificationPage() {
        return "staff-verification";
    }
    @GetMapping("/station-grid")
    public String showStationVehiclesGridPage() {
        return "staff-station-grid";
    }
}

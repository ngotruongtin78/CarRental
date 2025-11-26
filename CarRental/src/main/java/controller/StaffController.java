package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @GetMapping("/station-grid")
    public String showStationGridPage() {
        return "staff-station-grid";
    }

    @GetMapping("/handover")
    public String showHandoverPage() {
        return "staff-handover";
    }

    @GetMapping("/deliver")
    public String showDeliverVehiclePage() {
        return "staff-deliver";
    }

    @GetMapping("/return")
    public String showReturnVehiclePage() {
        return "staff-return-vehicle";
    }

    @GetMapping("/receive")
    public String showReceiveVehiclePageLegacy() {
        return "staff-return-vehicle";
    }

    @GetMapping("/verify")
    public String showVerificationPage() {
        return "staff-verification";
    }

    @GetMapping("/vehicle-reports")
    public String showReportsPage() {
        return "vehicle-reports";
    }
}
package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RentalPageController {

    @GetMapping("/lichsuthue")
    public String rentalHistoryPage() {
        return "lichsuthue";
    }

    @GetMapping("/thanhtoan")
    public String paymentPage() {
        return "thanhtoan";
    }

    @GetMapping("/sepay-qr")
    public String sepayQrPage() {
        return "sepay-qr";
    }
}

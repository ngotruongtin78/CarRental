package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/datxe")
public class BookingController {

    @GetMapping
    public String showBookingPage() {
        return "user-datxe";
    }

}
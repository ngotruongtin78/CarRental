package CarRental.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    @GetMapping("/ho-so")
    public String showProfilePage() {
        return "user-hosocanhan";
    }

    @GetMapping("/quy-trinh-ev")
    public String showWorkflowPage() {
        return "renter-workflow";
    }
}
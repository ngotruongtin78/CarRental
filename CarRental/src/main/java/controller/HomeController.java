package CarRental.example.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String showHomePage(Authentication authentication, Model model) {
        if (authentication != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        return "user-home";
    }
}

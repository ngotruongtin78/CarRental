package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(User user,
                           @RequestParam("confirmPassword") String confirmPassword,
                           Model model) {

        if (userRepo.findByUsername(user.getUsername()) != null) {
            model.addAttribute("errorUsername", "Tên đăng nhập đã tồn tại!");
            return "register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("errorConfirmPassword", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("username", user.getUsername());
            return "register";
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);

        return "redirect:/login?registered=true";
    }
}
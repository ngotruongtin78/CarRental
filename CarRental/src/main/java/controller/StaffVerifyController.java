package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
public class StaffVerifyController {

    private final UserRepository userRepo;

    public StaffVerifyController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/verify-user/{id}")
    public String verifyUser(@PathVariable("id") String id) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return "USER_NOT_FOUND";

        user.setVerified(true);
        user.setVerificationRequested(false);
        userRepo.save(user);

        return "USER_VERIFIED_SUCCESSFULLY";
    }
}

package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/renter")
public class RenterController {

    private final UserRepository repo;

    public RenterController(UserRepository repo) {
        this.repo = repo;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @PostMapping("/upload-license")
    public String uploadLicense(@RequestParam("file") MultipartFile file) throws Exception {

        String username = getCurrentUsername();
        if (username == null) return "Unauthorized";

        User user = repo.findByUsername(username);
        if (user == null) return "User not found";

        user.setLicenseData(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
        repo.save(user);

        return "Upload license success";
    }

    @PostMapping("/upload-idcard")
    public String uploadIdCard(@RequestParam("file") MultipartFile file) throws Exception {

        String username = getCurrentUsername();
        if (username == null) return "Unauthorized";

        User user = repo.findByUsername(username);
        if (user == null) return "User not found";

        user.setIdCardData(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
        repo.save(user);

        return "Upload idcard success";
    }

    @GetMapping("/profile")
    public User profile() {
        String username = getCurrentUsername();
        if (username == null) return null;

        User user = repo.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}

package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
    public ResponseEntity<?> uploadLicense(@RequestParam("file") MultipartFile file) {
        try {
            String username = getCurrentUsername();
            if (username == null) return ResponseEntity.status(401).body("Unauthorized");

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File trống");
            }

            User user = repo.findByUsername(username);
            if (user == null) return ResponseEntity.status(404).body("User not found");

            user.setLicenseData(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
            repo.save(user);

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload license failed");
        }
    }

    @PostMapping("/request-verification")
    public ResponseEntity<?> requestVerification() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");

        User user = repo.findByUsername(username);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        if (user.isVerificationRequested() || user.isVerified()) {
            return ResponseEntity.ok("ALREADY_REQUESTED_OR_VERIFIED");
        }

        user.setVerificationRequested(true);
        repo.save(user);

        return ResponseEntity.ok("REQUEST_SUBMITTED");
    }

    @PostMapping("/request-verification")
    public ResponseEntity<?> requestVerification() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");

        User user = repo.findByUsername(username);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        if (user.isVerificationRequested() || user.isVerified()) {
            return ResponseEntity.ok("ALREADY_REQUESTED_OR_VERIFIED");
        }

        user.setVerificationRequested(true);
        repo.save(user);

        return ResponseEntity.ok("REQUEST_SUBMITTED");
    }

    @PostMapping("/upload-idcard")
    public ResponseEntity<?> uploadIdCard(@RequestParam("file") MultipartFile file) {
        try {
            String username = getCurrentUsername();
            if (username == null) return ResponseEntity.status(401).body("Unauthorized");

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File trống");
            }

            User user = repo.findByUsername(username);
            if (user == null) return ResponseEntity.status(404).body("User not found");

            user.setIdCardData(new Binary(BsonBinarySubType.BINARY, file.getBytes()));
            repo.save(user);

            return ResponseEntity.ok(Map.of("status", "OK"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload idcard failed");
        }
    }

    @GetMapping("/verification-status")
    public ResponseEntity<?> verificationStatus() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");

        User user = repo.findByUsername(username);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        return ResponseEntity.ok(Map.of(
                "licenseUploaded", user.getLicenseData() != null,
                "idCardUploaded", user.getIdCardData() != null,
                "verificationRequested", user.isVerificationRequested(),
                "verified", user.isVerified()
        ));
    }

    @GetMapping("/verification-status")
    public ResponseEntity<?> verificationStatus() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(401).body("Unauthorized");

        User user = repo.findByUsername(username);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        return ResponseEntity.ok(Map.of(
                "licenseUploaded", user.getLicenseData() != null,
                "idCardUploaded", user.getIdCardData() != null,
                "verificationRequested", user.isVerificationRequested(),
                "verified", user.isVerified()
        ));
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

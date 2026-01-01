package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.Base64;

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

    private ResponseEntity<User> resolveCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).build();
        }

        User user = repo.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(user);
    }

    private String toDataUri(byte[] binaryData) {
        if (binaryData == null) return null;
        String base64 = Base64.getEncoder().encodeToString(binaryData);
        return "data:image/png;base64," + base64;
    }

    private Map<String, Object> buildDocumentPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("licenseData", toDataUri(user.getLicenseData()));
        payload.put("idCardData", toDataUri(user.getIdCardData()));
        payload.put("licenseUploaded", user.getLicenseData() != null);
        payload.put("idCardUploaded", user.getIdCardData() != null);
        return payload;
    }

    private ResponseEntity<?> storeDocument(MultipartFile file, Consumer<User> setter) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File trống");
            }

            ResponseEntity<User> resolved = resolveCurrentUser();
            if (!resolved.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(resolved.getStatusCode()).build();
            }

            User user = resolved.getBody();
            setter.accept(user);
            repo.save(user);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "licenseUploaded", user.getLicenseData() != null,
                    "idCardUploaded", user.getIdCardData() != null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload thất bại");
        }
    }

    private ResponseEntity<?> removeDocument(Consumer<User> clearer) {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).body("Unauthorized");
        }

        User user = resolved.getBody();
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }
        clearer.accept(user);
        repo.save(user);

        return ResponseEntity.ok(buildDocumentPayload(user));
    }

    @PostMapping("/upload-license")
    public ResponseEntity<?> uploadLicense(@RequestParam("file") MultipartFile file) {
        byte[] licenseBinary;
        try {
            licenseBinary = file.getBytes();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload thất bại");
        }

        return storeDocument(file, user -> user.setLicenseData(licenseBinary));
    }

    @PostMapping("/upload-idcard")
    public ResponseEntity<?> uploadIdCard(@RequestParam("file") MultipartFile file) {
        byte[] idBinary;
        try {
            idBinary = file.getBytes();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload thất bại");
        }

        return storeDocument(file, user -> user.setIdCardData(idBinary));
    }

    @DeleteMapping("/documents/{type}")
    public ResponseEntity<?> deleteDocument(@PathVariable("type") String type) {
        if ("license".equalsIgnoreCase(type)) {
            return removeDocument(user -> user.setLicenseData(null));
        } else if ("idcard".equalsIgnoreCase(type)) {
            return removeDocument(user -> user.setIdCardData(null));
        }

        return ResponseEntity.badRequest().body("Loại giấy tờ không hợp lệ");
    }

    @PostMapping("/request-verification")
    public ResponseEntity<?> requestVerification() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).body("Unauthorized");
        }

        User user = resolved.getBody();
        if (user.isVerificationRequested() || user.isVerified()) {
            return ResponseEntity.ok("ALREADY_REQUESTED_OR_VERIFIED");
        }

        user.setVerificationRequested(true);
        repo.save(user);

        return ResponseEntity.ok("REQUEST_SUBMITTED");
    }

    @GetMapping("/verification-status")
    public ResponseEntity<?> verificationStatus() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).body("Unauthorized");
        }

        User user = resolved.getBody();

        return ResponseEntity.ok(Map.of(
                "licenseUploaded", user.getLicenseData() != null,
                "idCardUploaded", user.getIdCardData() != null,
                "verificationRequested", user.isVerificationRequested(),
                "verified", user.isVerified()
        ));
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getUploadedDocuments() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).body("Unauthorized");
        }

        User user = resolved.getBody();

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        return ResponseEntity.ok(buildDocumentPayload(user));
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

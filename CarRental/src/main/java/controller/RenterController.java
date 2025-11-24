package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.bson.BsonBinarySubType;
import org.bson.types.Binary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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

    @PostMapping("/upload-license")
    public ResponseEntity<?> uploadLicense(@RequestParam("file") MultipartFile file) {
        Binary licenseBinary;
        try {
            licenseBinary = new Binary(BsonBinarySubType.BINARY, file.getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload thất bại");
        }

        return storeDocument(file, user -> user.setLicenseData(licenseBinary));
    }

    @PostMapping("/upload-idcard")
    public ResponseEntity<?> uploadIdCard(@RequestParam("file") MultipartFile file) {
        Binary idBinary;
        try {
            idBinary = new Binary(BsonBinarySubType.BINARY, file.getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Upload thất bại");
        }

        return storeDocument(file, user -> user.setIdCardData(idBinary));
    }

    @DeleteMapping("/delete-license")
    public ResponseEntity<?> deleteLicense() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).build();
        }

        User user = resolved.getBody();
        if (user != null) {
            user.setLicenseData(null);
            repo.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "status", "REMOVED",
                "licenseUploaded", false,
                "idCardUploaded", user != null && user.getIdCardData() != null
        ));
    }

    @DeleteMapping("/delete-idcard")
    public ResponseEntity<?> deleteIdCard() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).build();
        }

        User user = resolved.getBody();
        if (user != null) {
            user.setIdCardData(null);
            repo.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "status", "REMOVED",
                "licenseUploaded", user != null && user.getLicenseData() != null,
                "idCardUploaded", false
        ));
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

    private static final Pattern STATUS_PATTERN = Pattern.compile("Pending|InProgress|Done");

    private ResponseEntity<User> updateWorkflowStatus(BiConsumer<User, String> setter,
                                                      String status) {
        if (status == null || !STATUS_PATTERN.matcher(status).matches()) {
            return ResponseEntity.badRequest().build();
        }

        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).build();
        }

        User user = resolved.getBody();
        setter.accept(user, status);
        repo.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/workflow")
    public ResponseEntity<?> workflow() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).body("Unauthorized");
        }

        User user = resolved.getBody();

        return ResponseEntity.ok(Map.of(
                "checkin", user.getCheckinStatus(),
                "contract", user.getContractStatus(),
                "handover", user.getHandoverStatus(),
                "returning", user.getReturnStatus(),
                "payment", user.getPaymentStatus()
        ));
    }

    @PostMapping("/workflow/checkin")
    public ResponseEntity<User> updateCheckin(@RequestParam String status) {
        return updateWorkflowStatus(User::setCheckinStatus, status);
    }

    @PostMapping("/workflow/contract")
    public ResponseEntity<User> updateContract(@RequestParam String status) {
        return updateWorkflowStatus(User::setContractStatus, status);
    }

    @PostMapping("/workflow/handover")
    public ResponseEntity<User> updateHandover(@RequestParam String status) {
        return updateWorkflowStatus(User::setHandoverStatus, status);
    }

    @PostMapping("/workflow/returning")
    public ResponseEntity<User> updateReturn(@RequestParam String status) {
        return updateWorkflowStatus(User::setReturnStatus, status);
    }

    @PostMapping("/workflow/payment")
    public ResponseEntity<User> updatePayment(@RequestParam String status) {
        return updateWorkflowStatus(User::setPaymentStatus, status);
    }

    private ResponseEntity<byte[]> buildDocumentResponse(Binary binary) {
        if (binary == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = binary.getData();
        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        String detectedType = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            detectedType = URLConnection.guessContentTypeFromStream(bais);
        } catch (Exception ignored) {
        }

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, detectedType != null ? detectedType : "application/octet-stream")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    @GetMapping("/license-image")
    public ResponseEntity<byte[]> licenseImage() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).build();
        }

        User user = resolved.getBody();
        return buildDocumentResponse(user != null ? user.getLicenseData() : null);
    }

    @GetMapping("/idcard-image")
    public ResponseEntity<byte[]> idCardImage() {
        ResponseEntity<User> resolved = resolveCurrentUser();
        if (!resolved.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resolved.getStatusCode()).build();
        }

        User user = resolved.getBody();
        return buildDocumentResponse(user != null ? user.getIdCardData() : null);
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

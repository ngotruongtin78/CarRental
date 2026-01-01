package CarRental.example.controller;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
public class StaffVerifyController {

    private final UserRepository userRepo;

    public StaffVerifyController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Lấy danh sách người dùng (role = ROLE_USER) chưa xác thực (verified = false)
     * Trả về danh sách các loại giấy tờ mà user đó có
     */
    @GetMapping("/verifications/pending")
    public List<Map<String, Object>> getPendingVerifications() {
        List<User> allUsers = userRepo.findAll();

        return allUsers.stream()
                .filter(user -> "ROLE_USER".equals(user.getRole()))
                .filter(user -> !user.isVerified())
                .flatMap(user -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    boolean hasLicense = user.getLicenseData() != null;
                    boolean hasIdCard = user.getIdCardData() != null;

                    if (!hasLicense && !hasIdCard) {
                        // Nếu không có giấy tờ nào, vẫn thêm vào danh sách nhưng docType = "Chưa nộp"
                        Map<String, Object> notSubmittedEntry = new LinkedHashMap<>();
                        notSubmittedEntry.put("userId", user.getId());
                        notSubmittedEntry.put("username", user.getUsername());
                        notSubmittedEntry.put("docType", "Chưa nộp");
                        notSubmittedEntry.put("submittedAt", "");
                        notSubmittedEntry.put("verificationRequested", user.isVerificationRequested());
                        notSubmittedEntry.put("hasLicense", false);
                        notSubmittedEntry.put("hasIdCard", false);
                        results.add(notSubmittedEntry);
                    } else if (hasLicense && hasIdCard) {
                        // Nếu có cả 2, gộp thành 1 dòng
                        Map<String, Object> combinedEntry = new LinkedHashMap<>();
                        combinedEntry.put("userId", user.getId());
                        combinedEntry.put("username", user.getUsername());
                        combinedEntry.put("docType", "GPLX, CMND/CCCD");
                        combinedEntry.put("submittedAt", formatTimeAgo(user.getUpdatedAt()));
                        combinedEntry.put("verificationRequested", user.isVerificationRequested());
                        combinedEntry.put("hasLicense", true);
                        combinedEntry.put("hasIdCard", true);
                        results.add(combinedEntry);
                    } else if (hasLicense) {
                        // Chỉ có licenseData
                        Map<String, Object> licenseEntry = new LinkedHashMap<>();
                        licenseEntry.put("userId", user.getId());
                        licenseEntry.put("username", user.getUsername());
                        licenseEntry.put("docType", "GPLX");
                        licenseEntry.put("submittedAt", formatTimeAgo(user.getUpdatedAt()));
                        licenseEntry.put("verificationRequested", user.isVerificationRequested());
                        licenseEntry.put("hasLicense", true);
                        licenseEntry.put("hasIdCard", false);
                        results.add(licenseEntry);
                    } else {
                        // Chỉ có idCardData
                        Map<String, Object> idCardEntry = new LinkedHashMap<>();
                        idCardEntry.put("userId", user.getId());
                        idCardEntry.put("username", user.getUsername());
                        idCardEntry.put("docType", "CMND/CCCD");
                        idCardEntry.put("submittedAt", formatTimeAgo(user.getUpdatedAt()));
                        idCardEntry.put("verificationRequested", user.isVerificationRequested());
                        idCardEntry.put("hasLicense", false);
                        idCardEntry.put("hasIdCard", true);
                        results.add(idCardEntry);
                    }

                    return results.stream();
                })
                .sorted((a, b) -> {
                    // Sắp xếp theo thời gian cập nhật mới nhất đến cũ
                    String timeA = a.get("submittedAt").toString();
                    String timeB = b.get("submittedAt").toString();
                    return timeB.compareTo(timeA);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết người dùng kèm thông tin giấy tờ (Base64)
     */
    @GetMapping("/verifications/detail/{userId}")
    public Map<String, Object> getVerificationDetail(@PathVariable("userId") String userId) {
        Optional<User> userOpt = userRepo.findById(userId);

        if (userOpt.isEmpty()) {
            return Collections.singletonMap("error", "User not found");
        }

        User user = userOpt.get();
        Map<String, Object> detail = new LinkedHashMap<>();

        detail.put("userId", user.getId());
        detail.put("username", user.getUsername());

        // Chuyển byte array thành Base64
        if (user.getLicenseData() != null) {
            String licenseBase64 = Base64.getEncoder().encodeToString(user.getLicenseData());
            detail.put("licenseData", "data:image/png;base64," + licenseBase64);
        } else {
            detail.put("licenseData", null);
        }

        if (user.getIdCardData() != null) {
            String idCardBase64 = Base64.getEncoder().encodeToString(user.getIdCardData());
            detail.put("idCardData", "data:image/png;base64," + idCardBase64);
        } else {
            detail.put("idCardData", null);
        }

        return detail;
    }

    /**
     * Xác thực hoặc từ chối người dùng
     */
    @PostMapping("/verifications/process")
    public Map<String, String> processVerification(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Boolean approved = (Boolean) request.get("approved");

        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return Collections.singletonMap("status", "USER_NOT_FOUND");
        }

        User user = userOpt.get();

        if (approved) {
            user.setVerified(true);
            user.setVerificationRequested(false);
            user.setUpdatedAt(new Date());
            userRepo.save(user);
            return Collections.singletonMap("status", "APPROVED");
        } else {
            // Từ chối: xóa dữ liệu giấy tờ
            user.setLicenseData(null);
            user.setIdCardData(null);
            user.setVerificationRequested(false);
            user.setUpdatedAt(new Date());
            userRepo.save(user);
            return Collections.singletonMap("status", "DENIED");
        }
    }

    @PostMapping("/verify-user/{id}")
    public String verifyUser(@PathVariable("id") String id) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return "USER_NOT_FOUND";

        user.setVerified(true);
        user.setVerificationRequested(false);
        user.setUpdatedAt(new Date());
        userRepo.save(user);

        return "USER_VERIFIED_SUCCESSFULLY";
    }

    /**
     * Định dạng thời gian thành "X phút trước", "X giờ trước", etc.
     */
    private String formatTimeAgo(Date date) {
        if (date == null) {
            return "";
        }

        long now = System.currentTimeMillis();
        long diffInMillis = now - date.getTime();

        long minutes = diffInMillis / (60 * 1000);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";

        long hours = minutes / 60;
        if (hours < 24) return hours + " giờ trước";

        long days = hours / 24;
        return days + " ngày trước";
    }
}

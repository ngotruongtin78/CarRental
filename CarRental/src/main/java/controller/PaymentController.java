package CarRental.example.controller;

import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.service.VehicleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Value("${sepay.merchantId:}")
    private String merchantId;

    @Value("${sepay.secretKey:}")
    private String secretKey;

    @Value("${sepay.endpoint:https://api.sepay.vn/payment}")
    private String sepayEndpoint;

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;

    public PaymentController(RentalRecordRepository rentalRepo, VehicleRepository vehicleRepository, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
    }

    private boolean expireIfNeeded(RentalRecord record) {
        if (record == null) return false;

        boolean pending = "PENDING_PAYMENT".equalsIgnoreCase(record.getStatus());
        boolean expired = record.getHoldExpiresAt() != null && LocalDateTime.now().isAfter(record.getHoldExpiresAt());
        if (pending && expired) {
            record.setStatus("CANCELLED");
            record.setPaymentStatus("EXPIRED");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.releaseHold(record.getVehicleId(), record.getId());
            return true;
        }
        return false;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thanh toán");
        }
        String rentalId = (String) req.get("rentalId");
        if (rentalId == null || rentalId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu mã chuyến thuê");
        }

        if (merchantId == null || secretKey == null || merchantId.isBlank() || secretKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Thiếu cấu hình SePay, vui lòng bổ sung merchantId/secretKey");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
        }

        if (expireIfNeeded(record)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("Đơn đặt đã hết hạn thanh toán. Vui lòng đặt xe lại.");
        }

        if (record.getPaymentStatus() != null && "PAID".equalsIgnoreCase(record.getPaymentStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chuyến thuê đã thanh toán");
        }

        Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Xe không tồn tại");
        }

        int rentalDays = record.getRentalDays();
        if (rentalDays <= 0) {
            if (record.getStartDate() != null && record.getEndDate() != null) {
                rentalDays = (int) Math.max(1, ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate()));
            } else {
                rentalDays = 1;
            }
        }
        double amount = record.getTotal() > 0 ? record.getTotal() : rentalDays * vehicle.getPrice();
        record.setTotal(amount);
        record.setPaymentMethod("bank_transfer");
        record.setPaymentStatus("PENDING");
        record.setStatus("PENDING_PAYMENT");
        rentalRepo.save(record);

        Map<String, Object> payload = new LinkedHashMap<>();
        int orderCode = Math.abs(rentalId.hashCode());
        int amountInt = (int) Math.round(amount);
        String description = "Thanh toan don " + rentalId;
        String returnUrl = "http://localhost:8080/payment/return?rentalId=" + rentalId;
        String cancelUrl = "http://localhost:8080/payment/cancel?rentalId=" + rentalId;

        String signatureRaw = merchantId + "|" + orderCode + "|" + amountInt + "|" + description + "|" + returnUrl + "|" + cancelUrl;
        String signature = signPayload(signatureRaw);

        StringBuilder paymentUrl = new StringBuilder(sepayEndpoint);
        paymentUrl.append(paymentUrl.toString().contains("?") ? "&" : "?")
                .append("merchantId=").append(urlEncode(merchantId))
                .append("&orderCode=").append(orderCode)
                .append("&amount=").append(amountInt)
                .append("&description=").append(urlEncode(description))
                .append("&returnUrl=").append(urlEncode(returnUrl))
                .append("&cancelUrl=").append(urlEncode(cancelUrl))
                .append("&signature=").append(urlEncode(signature));

        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=" + urlEncode(paymentUrl.toString());

        payload.put("orderCode", orderCode);
        payload.put("amount", amountInt);
        payload.put("description", description);
        payload.put("checkoutUrl", paymentUrl.toString());
        payload.put("qrCodeUrl", qrCodeUrl);
        payload.put("rentalId", rentalId);
        payload.put("status", "OK");

        return ResponseEntity.ok(payload);
    }

    private String signPayload(String raw) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String urlEncode(String data) {
        try {
            return URLEncoder.encode(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return data;
        }
    }

    @GetMapping("/return")
    public RedirectView paymentReturn(@RequestParam("rentalId") String rentalId,
                                      @RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "amount", required = false) String amount) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            if (expireIfNeeded(record)) {
                RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&cancel=1");
                redirectView.setExposeModelAttributes(false);
                return redirectView;
            }
            record.setPaymentStatus("PAID");
            if (amount != null) {
                try { record.setTotal(Double.parseDouble(amount)); } catch (NumberFormatException ignored) {}
            }
            record.setPaidAt(LocalDateTime.now());
            record.setStatus("PAID");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.markRented(record.getVehicleId(), rentalId);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&success=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    @GetMapping("/cancel")
    public RedirectView paymentCancel(@RequestParam("rentalId") String rentalId) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            record.setPaymentStatus("CANCELLED");
            record.setStatus("CANCELLED");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.releaseHold(record.getVehicleId(), rentalId);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&cancel=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}

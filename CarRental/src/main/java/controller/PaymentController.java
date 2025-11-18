package CarRental.example.controller;

import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.view.RedirectView;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepository;

    public PaymentController(RentalRecordRepository rentalRepo, VehicleRepository vehicleRepository) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepository = vehicleRepository;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String rentalId = (String) req.get("rentalId");
        if (rentalId == null || rentalId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu mã chuyến thuê");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
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
        rentalRepo.save(record);

        try {
            RestTemplate rest = new RestTemplate();

            Map<String, Object> body = new HashMap<>();
            int orderCode = Math.abs(rentalId.hashCode());
            body.put("orderCode", orderCode);
            int amountInt = (int) Math.round(amount);
            body.put("amount", amountInt);
            String description = "Thanh toan don " + rentalId;
            body.put("description", description);
            String returnUrl = "http://localhost:8080/payment/return?rentalId=" + rentalId;
            String cancelUrl = "http://localhost:8080/payment/cancel?rentalId=" + rentalId;
            body.put("returnUrl", returnUrl);
            body.put("cancelUrl", cancelUrl);
            body.put("buyerName", username);
            body.put("items", List.of(
                    Map.of(
                            "name", vehicle.getBrand() + " (" + vehicle.getPlate() + ")",
                            "quantity", 1,
                            "price", amountInt
                    )
            ));

            String signatureRaw = orderCode + "|" + amountInt + "|" + description + "|" + returnUrl + "|" + cancelUrl;
            body.put("signature", signPayload(signatureRaw));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = rest.exchange(
                    "https://api-merchant.payos.vn/v2/payment-requests",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                String reason = response.getBody() != null ? response.getBody().toString() : Optional.ofNullable(HttpStatus.resolve(response.getStatusCode().value()))
                        .map(HttpStatus::getReasonPhrase)
                        .orElse(response.getStatusCode().toString());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Tạo QR thất bại: " + reason);
            }

            Map<String, Object> payload = response.getBody();
            Object dataObj = payload != null ? payload.get("data") : null;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderCode", orderCode);
            result.put("amount", amountInt);
            if (dataObj instanceof Map<?, ?> data) {
                result.put("checkoutUrl", data.get("checkoutUrl"));
                result.put("qrCode", data.get("qrCode"));
                result.put("qrCodeUrl", data.get("qrCodeUrl"));
            }
            result.put("status", payload != null ? payload.get("desc") : "OK");
            result.put("rentalId", rentalId);
            return ResponseEntity.ok(result);

        } catch (HttpStatusCodeException httpEx) {
            String responseBody = httpEx.getResponseBodyAsString();
            String reason = !responseBody.isEmpty() ? responseBody : Optional.ofNullable(HttpStatus.resolve(httpEx.getRawStatusCode()))
                    .map(HttpStatus::getReasonPhrase)
                    .orElse(httpEx.getStatusCode().toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Tạo QR thất bại: " + reason);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private String signPayload(String raw) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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

    @GetMapping("/return")
    public RedirectView paymentReturn(@RequestParam String rentalId, @RequestParam(required = false) String status, @RequestParam(required = false) String amount) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            record.setPaymentStatus("PAID");
            if (amount != null) {
                try { record.setTotal(Double.parseDouble(amount)); } catch (NumberFormatException ignored) {}
            }
            record.setPaidAt(LocalDateTime.now());
            rentalRepo.save(record);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&success=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    @GetMapping("/cancel")
    public RedirectView paymentCancel(@RequestParam String rentalId) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            record.setPaymentStatus("CANCELLED");
            rentalRepo.save(record);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&cancel=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}

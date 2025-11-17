package CarRental.example.controller;

import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.net.URI;

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
            body.put("amount", (int) Math.round(amount));
            body.put("description", "Thanh toan don " + rentalId);
            body.put("returnUrl", "http://localhost:8080/payment/return?rentalId=" + rentalId);
            body.put("cancelUrl", "http://localhost:8080/payment/cancel?rentalId=" + rentalId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.set("x-checksum-key", checksumKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = rest.exchange(
                    "https://api.payos.vn/v2/payment-requests",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<?, ?> payload = response.getBody();
            Map<String, Object> data = payload != null && payload.get("data") instanceof Map ? (Map<String, Object>) payload.get("data") : new HashMap<>();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderCode", orderCode);
            result.put("amount", (int) Math.round(amount));
            result.put("checkoutUrl", data.get("checkoutUrl"));
            result.put("qrCode", data.get("qrCode"));
            result.put("qrCodeUrl", data.get("qrCodeUrl"));
            result.put("status", payload != null ? payload.get("desc") : "OK");
            result.put("rentalId", rentalId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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

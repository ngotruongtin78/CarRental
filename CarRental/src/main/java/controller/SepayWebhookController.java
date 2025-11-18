package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.VehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/sepay")
public class SepayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookController.class);

    private final RentalRecordRepository rentalRepo;
    private final VehicleService vehicleService;

    public SepayWebhookController(RentalRecordRepository rentalRepo, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleService = vehicleService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        String description = Objects.toString(payload.get("description"), "");
        String rentalId = description;
        if (description.contains("#")) {
            rentalId = description.substring(description.indexOf('#') + 1).trim();
        }

        if (rentalId.isBlank()) {
            log.warn("Webhook thiếu description: {}", payload);
            return ResponseEntity.ok("OK");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            log.warn("Không tìm thấy đơn thuê {} từ webhook", rentalId);
            return ResponseEntity.ok("OK");
        }

        if (!"PAID".equalsIgnoreCase(record.getPaymentStatus())) {
            record.setPaymentStatus("PAID");
            record.setPaymentMethod("bank_transfer");
            record.setStatus("PAID");
            record.setHoldExpiresAt(null);
            if (payload.get("amount") instanceof Number amountNumber) {
                record.setTotal(amountNumber.doubleValue());
            }
            record.setPaidAt(LocalDateTime.now());
            rentalRepo.save(record);
            vehicleService.markRented(record.getVehicleId(), rentalId);
        }

        return ResponseEntity.ok("OK");
    }
}

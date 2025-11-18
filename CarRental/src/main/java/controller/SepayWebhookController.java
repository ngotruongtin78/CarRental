package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
public class SepayWebhookController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleService vehicleService;

    public SepayWebhookController(RentalRecordRepository rentalRepo, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleService = vehicleService;
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {

        String rentalId = payload.get("description").toString();

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) return ResponseEntity.ok("order-not-found");

        record.setPaymentMethod("bank_transfer");
        record.setStatus("PAID");
        record.setHoldExpiresAt(null);

        if (payload.get("amount") instanceof Number num) {
            record.setTotal(num.doubleValue());
        }

        record.setPaidAt(LocalDateTime.now());

        rentalRepo.save(record);
        vehicleService.markRented(record.getVehicleId(), rentalId);

        return ResponseEntity.ok("OK");
    }
}

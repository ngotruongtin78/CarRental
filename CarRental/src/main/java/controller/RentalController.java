package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.RentalRecordService;
import CarRental.example.service.SequenceGeneratorService;
import CarRental.example.service.VehicleService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/rental")
public class RentalController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;
    private final SequenceGeneratorService sequence;
    private final VehicleService vehicleService;
    private final RentalRecordService rentalRecordService;
    public RentalController(RentalRecordRepository rentalRepo,
                            VehicleRepository vehicleRepo,
                            SequenceGeneratorService sequence, VehicleService vehicleService,
                            RentalRecordService rentalRecordService) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
        this.sequence = sequence;
        this.vehicleService = vehicleService;
        this.rentalRecordService = rentalRecordService;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> req) {

        String vehicleId = (String) req.get("vehicleId");
        String stationId = (String) req.get("stationId");

        Vehicle v = vehicleRepo.findById(vehicleId).orElse(null);
        if (v == null) {
            return Map.of("error", "Vehicle not found");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("vehicleId", v.getId());
        data.put("vehicleName", v.getBrand());
        data.put("price", v.getPrice());
        data.put("stationId", stationId);

        return data;
    }

    @PostMapping(value = "/book", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bookRental(@RequestParam("vehicleId") String vehicleId,
                                        @RequestParam(value = "stationId", required = false) String stationId) {

        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElse(null);
        if (vehicle == null || !vehicle.isAvailable()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vehicle not available");
        }

        long seq = sequence.getNextSequence("rentalCounter");
        String rentalId = "rental" + seq;

        RentalRecord record = new RentalRecord();
        record.setId(rentalId);
        record.setUsername(username);
        record.setVehicleId(vehicleId);
        record.setStationId(stationId);
        record.setStartTime(LocalDateTime.now());
        record.setTotal(vehicle.getPrice());
        record.setStatus("BOOKED");

        rentalRepo.save(record);
        vehicleRepo.updateAvailable(vehicleId, false);

        return ResponseEntity.ok(record);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        return ResponseEntity.ok(rentalRepo.findByUsername(username));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        return ResponseEntity.ok(rentalRecordService.calculateStats(username));
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getRental(@PathVariable String rentalId) {
        String username = getCurrentUsername();
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        if (username == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }
        return ResponseEntity.ok(record);
    }

    @PostMapping("/{rentalId}/sign-contract")
    public Map<String, Object> signContract(@PathVariable String rentalId) {
        String username = getCurrentUsername();
        RentalRecord record = rentalRecordService.signContract(rentalId, username);
        if (record == null) return Map.of("error", "Rental not found or unauthorized");
        return Map.of("status", "SIGNED", "contractSigned", true);
    }

    @PostMapping("/{rentalId}/check-in")
    public Map<String, Object> checkIn(@PathVariable String rentalId, @RequestBody(required = false) Map<String, String> body) {
        String username = getCurrentUsername();
        String notes = body != null ? body.getOrDefault("notes", "") : "";

        RentalRecord record = rentalRecordService.checkIn(rentalId, username, notes);
        if (record == null) return Map.of("error", "Rental not found or unauthorized");

        vehicleService.updateAvailable(record.getVehicleId(), false);
        return Map.of(
                "status", record.getStatus(),
                "checkinNotes", record.getCheckinNotes(),
                "startTime", record.getStartTime()
        );
    }

    @PostMapping("/{rentalId}/return")
    public Map<String, Object> requestReturn(@PathVariable String rentalId, @RequestBody(required = false) Map<String, String> body) {
        String username = getCurrentUsername();
        String notes = body != null ? body.getOrDefault("notes", "") : "";

        RentalRecord record = rentalRecordService.requestReturn(rentalId, username, notes);
        if (record == null) return Map.of("error", "Rental not found or unauthorized");

        return Map.of(
                "status", record.getStatus(),
                "returnNotes", record.getReturnNotes(),
                "endTime", record.getEndTime()
        );
    }
}

package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.User;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.UserRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/rental")
public class RentalController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;
    private final SequenceGeneratorService sequence;
    private final VehicleService vehicleService;
    private final RentalRecordService rentalRecordService;
    private final UserRepository userRepository;
    public RentalController(RentalRecordRepository rentalRepo,
                            VehicleRepository vehicleRepo,
                            SequenceGeneratorService sequence, VehicleService vehicleService,
                            RentalRecordService rentalRecordService,
                            UserRepository userRepository) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
        this.sequence = sequence;
        this.vehicleService = vehicleService;
        this.rentalRecordService = rentalRecordService;
        this.userRepository = userRepository;
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
                                        @RequestParam("stationId") String stationId,
                                        @RequestParam("startDate") String startDateStr,
                                        @RequestParam("endDate") String endDateStr,
                                        @RequestParam(value = "distanceKm", required = false) Double distanceKm) {

        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vehicle not available");
        }

        String bookingState = vehicle.getBookingStatus() == null ? "AVAILABLE" : vehicle.getBookingStatus();
        if ("RENTED".equalsIgnoreCase(bookingState)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vehicle already rented");
        }
        if ("PENDING_PAYMENT".equalsIgnoreCase(bookingState) && vehicle.getPendingRentalId() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Xe đang chờ thanh toán");
        }

        final LocalDate startDate;
        final LocalDate endDate;
        try {
            startDate = startDateStr != null && !startDateStr.isBlank()
                    ? LocalDate.parse(startDateStr)
                    : LocalDate.now();
            endDate = endDateStr != null && !endDateStr.isBlank()
                    ? LocalDate.parse(endDateStr)
                    : startDate;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format");
        }

        final long daySpan = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int rentalDays = (int) Math.max(1, daySpan);

        long seq = sequence.getNextSequence("rentalCounter");
        String rentalId = "rental" + seq;

        RentalRecord record = new RentalRecord();
        record.setId(rentalId);
        record.setUsername(username);
        record.setVehicleId(vehicleId);
        record.setStationId(stationId);
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        record.setRentalDays(rentalDays);
        record.setDistanceKm(distanceKm != null ? distanceKm : 0);
        record.setStartTime(startDate.atStartOfDay());
        record.setEndTime(endDate.plusDays(1).atStartOfDay());
        record.setTotal(vehicle.getPrice() * rentalDays);
        record.setStatus("PENDING_PAYMENT");
        record.setPaymentStatus("PENDING");

        rentalRepo.save(record);
        boolean held = vehicleService.markPendingPayment(vehicleId, rentalId);
        if (!held) {
            record.setStatus("CANCELLED");
            record.setPaymentStatus("CANCELLED");
            rentalRepo.save(record);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Xe đang chờ thanh toán");
        }

        return ResponseEntity.ok(record);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history() {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        return ResponseEntity.ok(rentalRecordService.getHistoryDetails(username));
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

    @PostMapping("/{rentalId}/payment")
    public ResponseEntity<?> confirmPayment(@PathVariable String rentalId, @RequestBody Map<String, String> body) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        String method = body.getOrDefault("method", "");
        if (!method.equals("cash") && !method.equals("bank_transfer")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payment method");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        }

        Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vehicle missing for rental");
        }

        int rentalDays = record.getRentalDays() > 0 ? record.getRentalDays() : 1;
        double calculatedTotal = rentalDays * vehicle.getPrice();
        record.setTotal(calculatedTotal);
        record.setPaymentMethod(method);
        record.setPaymentStatus(method.equals("cash") ? "PAY_AT_STATION" : "BANK_TRANSFER");
        record.setStatus("PENDING_PAYMENT");

        if (method.equals("bank_transfer")) {
            User user = userRepository.findByUsername(username);
            if (user == null || user.getLicenseData() == null || user.getIdCardData() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Vui lòng tải lên CCCD và GPLX để thanh toán chuyển khoản");
            }
        }

        rentalRepo.save(record);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentStatus", record.getPaymentStatus());
        response.put("total", calculatedTotal);
        response.put("rentalDays", rentalDays);
        response.put("paymentMethod", method);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/cancel")
    public ResponseEntity<?> cancelRental(@PathVariable String rentalId) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        }

        record.setStatus("CANCELLED");
        record.setPaymentStatus("CANCELLED");
        rentalRepo.save(record);
        vehicleService.releaseHold(record.getVehicleId(), rentalId);
        return ResponseEntity.ok(Map.of("status", "CANCELLED"));
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

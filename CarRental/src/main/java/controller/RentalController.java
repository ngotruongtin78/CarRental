package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.User;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.StationRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/rental")
public class RentalController {

    private static final long MAX_PHOTO_SIZE = 10 * 1024 * 1024; // 10MB

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;
    private final StationRepository stationRepository;
    private final SequenceGeneratorService sequence;
    private final VehicleService vehicleService;
    private final RentalRecordService rentalRecordService;
    private final UserRepository userRepository;

    public RentalController(RentalRecordRepository rentalRepo,
                            VehicleRepository vehicleRepo,
                            StationRepository stationRepository,
                            SequenceGeneratorService sequence, VehicleService vehicleService,
                            RentalRecordService rentalRecordService,
                            UserRepository userRepository) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
        this.stationRepository = stationRepository;
        this.sequence = sequence;
        this.vehicleService = vehicleService;
        this.rentalRecordService = rentalRecordService;
        this.userRepository = userRepository;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null; // Valid - no photo provided
        }
        if (photo.getSize() > MAX_PHOTO_SIZE) {
            return "Ảnh quá lớn. Kích thước tối đa là 10MB.";
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "File phải là ảnh (JPEG, PNG, etc.).";
        }
        return null; // Valid
    }

    // Kiểm tra và hủy đơn nếu hết hạn giữ xe
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

        // Kiểm tra trạng thái xe
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
        LocalDateTime actualStartTime = LocalDateTime.now();
        record.setStartTime(actualStartTime);
        record.setEndTime(actualStartTime.plusDays(rentalDays));
        double totalAmount = vehicle.getPrice() * rentalDays;
        record.setTotal(totalAmount);
        record.setStatus("PENDING_PAYMENT");
        record.setPaymentStatus("PENDING");
        record.setCreatedAt(LocalDateTime.now());
        record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        // Tính số tiền đặt cọc cần thiết (30% tổng giá)
        double depositRequired = Math.round(totalAmount * 0.3 * 100.0) / 100.0;
        record.setDepositRequiredAmount(depositRequired);

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
    public ResponseEntity<?> getRental(@PathVariable("rentalId") String rentalId) {
        try {
            String username = getCurrentUsername();
            RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
            if (record == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
            if (username == null || !Objects.equals(record.getUsername(), username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }

            if (expireIfNeeded(record)) {
                return ResponseEntity.status(HttpStatus.GONE)
                        .body("Đơn đặt đã hết hạn thanh toán. Vui lòng đặt xe lại.");
            }

            Vehicle vehicle = null;
            if (record.getVehicleId() != null) {
                vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
            }
            var station = record.getStationId() != null
                    ? stationRepository.findById(record.getStationId()).orElse(null)
                    : null;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", record.getId());
            payload.put("username", record.getUsername());
            payload.put("vehicleId", record.getVehicleId());
            payload.put("stationId", record.getStationId());
            payload.put("startDate", record.getStartDate());
            payload.put("endDate", record.getEndDate());
            payload.put("rentalDays", record.getRentalDays());
            payload.put("distanceKm", record.getDistanceKm());
            payload.put("total", record.getTotal());
            payload.put("paymentMethod", record.getPaymentMethod());
            payload.put("paymentStatus", record.getPaymentStatus());
            payload.put("status", record.getStatus());
            payload.put("holdExpiresAt", record.getHoldExpiresAt());

            if (vehicle != null) {
                payload.put("vehicle", vehicle);
                payload.put("vehiclePrice", vehicle.getPrice());
                payload.put("vehicleBrand", vehicle.getBrand());
                payload.put("vehiclePlate", vehicle.getPlate());
            }
            if (station != null) {
                payload.put("station", station);
            }

            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể tải thông tin thanh toán, vui lòng thử lại");
        }
    }

    @PostMapping("/{rentalId}/payment")
    public ResponseEntity<?> confirmPayment(@PathVariable("rentalId") String rentalId, @RequestBody Map<String, String> body) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        final String paymentMethod = body.getOrDefault("method", "");
        if (!paymentMethod.equals("cash") && !paymentMethod.equals("bank_transfer")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payment method");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        }

        if (expireIfNeeded(record)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("Đơn đặt đã hết hạn thanh toán. Vui lòng đặt lại.");
        }

        Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vehicle missing for rental");
        }

        int rentalDays = record.getRentalDays() > 0 ? record.getRentalDays() : 1;
        double calculatedTotal = rentalDays * vehicle.getPrice();

        if ("bank_transfer".equals(paymentMethod)) {
            User user = userRepository.findByUsername(username);
            if (user == null || user.getLicenseData() == null || user.getIdCardData() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Vui lòng tải lên CCCD và GPLX để thanh toán chuyển khoản");
            }
        }

        record.setTotal(calculatedTotal);
        record.setPaymentMethod(paymentMethod);

        // Tính đặt cọc yêu cầu (30%)
        double depositRequired = Math.round(calculatedTotal * 0.3 * 100.0) / 100.0;
        record.setDepositRequiredAmount(depositRequired);

        if ("cash".equals(paymentMethod)) {
            // Tiền mặt: Chờ chuyển khoản đặt cọc 30%
            record.setPaymentStatus("DEPOSIT_PENDING");
            record.setStatus("PENDING_PAYMENT");
            record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); // Giữ 5 phút cho đến khi chuyển cọc
        } else {
            // Chuyển khoản: Chờ thanh toán
            record.setPaymentStatus("BANK_TRANSFER");
            record.setStatus("PENDING_PAYMENT");
            record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5)); // Giữ 5 phút cho đến khi chuyển tiền
        }

        rentalRepo.save(record);
        vehicleService.markPendingPayment(record.getVehicleId(), rentalId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentStatus", record.getPaymentStatus());
        response.put("total", calculatedTotal);
        response.put("rentalDays", rentalDays);
        response.put("paymentMethod", paymentMethod);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rentalId}/cancel")
    public ResponseEntity<?> cancelRental(@PathVariable("rentalId") String rentalId) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        }

        record.setStatus("CANCELLED");
        record.setPaymentStatus("CANCELLED");
        record.setHoldExpiresAt(null);
        rentalRepo.save(record);
        vehicleService.releaseHold(record.getVehicleId(), rentalId);
        return ResponseEntity.ok(Map.of("status", "CANCELLED"));
    }

    @PostMapping("/{rentalId}/sign-contract")
    public Map<String, Object> signContract(@PathVariable("rentalId") String rentalId) {
        String username = getCurrentUsername();
        RentalRecord record = rentalRecordService.signContract(rentalId, username);
        if (record == null) return Map.of("error", "Rental not found or unauthorized");
        return Map.of("status", "SIGNED", "contractSigned", true);
    }

    @PostMapping(value = "/{rentalId}/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> checkIn(
            @PathVariable("rentalId") String rentalId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude
    ) {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String validationError = validatePhoto(photo);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
        }

        byte[] photoData = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoData = photo.getBytes();
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể đọc file ảnh.");
            }
        }

        RentalRecord record = rentalRecordService.checkIn(rentalId, username, notes != null ? notes : "", photoData, latitude, longitude);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found or unauthorized");
        }

        vehicleService.updateAvailable(record.getVehicleId(), false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", record.getStatus());
        response.put("checkinNotes", record.getCheckinNotes());
        response.put("startTime", record.getStartTime());
        response.put("message", "Check-in thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{rentalId}/return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> requestReturn(
            @PathVariable("rentalId") String rentalId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude
    ) {
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String validationError = validatePhoto(photo);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
        }

        byte[] photoData = null;
        if (photo != null && !photo.isEmpty()) {
            try {
                photoData = photo.getBytes();
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Không thể đọc file ảnh.");
            }
        }

        RentalRecord record = rentalRecordService.requestReturn(rentalId, username, notes != null ? notes : "", photoData, latitude, longitude);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found or unauthorized");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", record.getStatus());
        response.put("returnNotes", record.getReturnNotes());
        response.put("endTime", record.getEndTime());
        response.put("message", "Yêu cầu trả xe thành công");
        return ResponseEntity.ok(response);
    }

    // API dành cho Admin: Lấy danh sách lịch sử chi tiết
    @GetMapping("/admin/all-history")
    public ResponseEntity<?> getAllHistoryForAdmin() {
        List<Map<String, Object>> records = rentalRecordService.getAllHistoryDetails();
        return ResponseEntity.ok(records);
    }

    // API dành cho Admin: Lấy chi tiết một đơn hàng
    @GetMapping("/admin/detail/{id}")
    public ResponseEntity<?> getRentalDetailForAdmin(@PathVariable("id") String id) {
        try {
            RentalRecord record = rentalRepo.findById(id).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Không tìm thấy đơn thuê"));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rental", record);

            // Lấy thông tin xe
            if (record.getVehicleId() != null) {
                response.put("vehicle", vehicleRepo.findById(record.getVehicleId()).orElse(null));
            }

            // Lấy thông tin trạm
            if (record.getStationId() != null) {
                response.put("station", stationRepository.findById(record.getStationId()).orElse(null));
            }

            // Lấy thông tin khách hàng
            if (record.getUsername() != null) {
                User user = userRepository.findByUsername(record.getUsername());
                if(user != null) {
                    user.setPassword(null); // Bảo mật
                    user.setLicenseData(null); // Giảm tải
                    user.setIdCardData(null);
                    response.put("customer", user);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
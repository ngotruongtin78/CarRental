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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/rental")
public class RentalController {

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

    private void expirePendingHoldsForUser(String username) {
        if (username == null) return;
        List<RentalRecord> rentals = rentalRepo.findByUsername(username);
        for (RentalRecord record : rentals) {
            expireIfNeeded(record);
        }
    }

    private double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double earthRadius = 6371000; // meters
        return earthRadius * c;
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

        if (startDate.isBefore(LocalDate.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ngày bắt đầu không được ở quá khứ");
        }
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
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
        record.setEndTime(endDate.plusDays(1).atStartOfDay());
        record.setTotal(vehicle.getPrice() * rentalDays);
        record.setStatus("PENDING_PAYMENT");
        record.setPaymentStatus("PENDING");
        record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));

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

        expirePendingHoldsForUser(username);
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
        record.setPaymentStatus(paymentMethod.equals("cash") ? "PAY_AT_STATION" : "BANK_TRANSFER");

        record.setStatus("PENDING_PAYMENT");

        if (paymentMethod.equals("cash")) {
            LocalDate holdStart = Optional.ofNullable(record.getStartDate()).orElse(LocalDate.now());
            LocalDateTime holdUntil = holdStart.atStartOfDay().plusDays(1);
            if (holdUntil.isBefore(LocalDateTime.now())) {
                holdUntil = LocalDateTime.now().plusDays(1);
            }
            record.setHoldExpiresAt(holdUntil);
            rentalRepo.save(record);
            vehicleService.markPendingPaymentHidden(record.getVehicleId(), rentalId);
        } else {
            record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
            rentalRepo.save(record);
            vehicleService.markPendingPayment(record.getVehicleId(), rentalId);
        }
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

    @PostMapping(value = "/{rentalId}/check-in", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<?> checkIn(
            @PathVariable("rentalId") String rentalId,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "notes", required = false) String notes,
            @RequestPart(value = "latitude", required = false) Double latitude,
            @RequestPart(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "latitude", required = false) Double latitudeParam,
            @RequestParam(value = "longitude", required = false) Double longitudeParam,
            @RequestBody(required = false) byte[] rawBody) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found");
        }

        if ("IN_PROGRESS".equalsIgnoreCase(record.getStatus())
                || "WAITING_INSPECTION".equalsIgnoreCase(record.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Bạn đã check-in hoặc đang trong quá trình thuê xe.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (record.getStartDate() != null && now.isBefore(record.getStartDate().atStartOfDay())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Chưa tới thời gian nhận xe trong lịch đặt trước.");
        }
        if (record.getEndDate() != null && now.isAfter(record.getEndDate().plusDays(1).atStartOfDay())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Đã quá thời gian nhận xe của chuyến này.");
        }

        if (!record.isContractSigned()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng đọc và chấp thuận hợp đồng trước khi check-in.");
        }

        if (latitude == null) latitude = latitudeParam;
        if (longitude == null) longitude = longitudeParam;

        if (latitude == null || longitude == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng bật định vị để xác nhận bạn đang ở trạm thuê.");
        }

        var station = record.getStationId() != null ? stationRepository.findById(record.getStationId()).orElse(null) : null;
        if (station == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không tìm thấy thông tin trạm thuê cho chuyến này.");
        }

        double distanceMeters = distanceInMeters(latitude, longitude, station.getLatitude(), station.getLongitude());
        if (Double.isNaN(distanceMeters) || distanceMeters > 50) {
            long rounded = Math.round(distanceMeters);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn cần có mặt trong bán kính 50m của trạm thuê để check-in. Khoảng cách hiện tại: " + rounded + "m.");
        }

        byte[] photoData = null;
        try {
            if (photo != null && !photo.isEmpty()) {
                photoData = photo.getBytes();
            } else if (rawBody != null && rawBody.length > 0) {
                photoData = rawBody;
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không đọc được ảnh check-in. Vui lòng thử lại.");
        }

        if (photoData == null || photoData.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng chụp hoặc tải ảnh tình trạng xe để check-in.");
        }

        RentalRecord updated = rentalRecordService.checkIn(
                rentalId,
                username,
                notes != null ? notes : "",
                photoData,
                latitude,
                longitude);
        if (updated == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found or unauthorized");

        vehicleService.updateAvailable(updated.getVehicleId(), false);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", updated.getStatus());
        response.put("checkinNotes", updated.getCheckinNotes());
        response.put("startTime", updated.getStartTime());
        response.put("photoUploaded", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{rentalId}/return", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<?> requestReturn(@PathVariable("rentalId") String rentalId,
                                           @RequestPart(value = "photo", required = false) MultipartFile photo,
                                           @RequestPart(value = "notes", required = false) String notes,
                                           @RequestPart(value = "latitude", required = false) Double latitude,
                                           @RequestPart(value = "longitude", required = false) Double longitude,
                                           @RequestParam(value = "latitude", required = false) Double latitudeParam,
                                           @RequestParam(value = "longitude", required = false) Double longitudeParam,
                                           @RequestBody(required = false) byte[] rawBody) {
        String username = getCurrentUsername();
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found or unauthorized");
        }

        if (!"IN_PROGRESS".equalsIgnoreCase(record.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Chỉ trả xe khi chuyến đang được thuê.");
        }

        if (latitude == null) latitude = latitudeParam;
        if (longitude == null) longitude = longitudeParam;

        if (latitude == null || longitude == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng bật định vị và đứng tại trạm để trả xe.");
        }

        var station = record.getStationId() != null ? stationRepository.findById(record.getStationId()).orElse(null) : null;
        if (station == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không tìm thấy thông tin trạm cho chuyến này.");
        }

        double distanceMeters = distanceInMeters(latitude, longitude, station.getLatitude(), station.getLongitude());
        if (Double.isNaN(distanceMeters) || distanceMeters > 50) {
            long rounded = Math.round(distanceMeters);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Vị trí nằm ngoài khu vực trạm hoặc sai trạm (cách khoảng " + rounded + "m).");
        }

        byte[] photoData = null;
        try {
            if (photo != null && !photo.isEmpty()) {
                photoData = photo.getBytes();
            } else if (rawBody != null && rawBody.length > 0) {
                photoData = rawBody;
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không đọc được ảnh trả xe. Vui lòng thử lại.");
        }

        if (photoData == null || photoData.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng chụp hoặc tải ảnh tình trạng xe khi trả.");
        }

        RentalRecord updated = rentalRecordService.requestReturn(
                rentalId,
                username,
                notes != null ? notes : "",
                photoData,
                latitude,
                longitude);
        if (updated == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rental not found or unauthorized");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", updated.getStatus());
        response.put("returnNotes", updated.getReturnNotes());
        response.put("endTime", updated.getEndTime());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/all-history")
    public ResponseEntity<?> getAllHistoryForAdmin() {
        List<RentalRecord> records = rentalRecordService.getAll();
        return ResponseEntity.ok(records);
    }
}
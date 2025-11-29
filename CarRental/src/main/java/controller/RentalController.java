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
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(23, 59, 59);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
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

    /**
     * Update rental dates (startDate, endDate)
     * Chỉ được phép khi chuyến chưa bắt đầu và chưa check-in
     * 
     * Smart payment logic:
     * - Case A: Đã thanh toán 100% cho số ngày cũ, tăng ngày → PENDING_EXTRA
     * - Case B: Đặt cọc tiền mặt (cash), tăng ngày → kiểm tra cọc đủ 30%
     * - Case C: Chuyển khoản nhưng chưa đủ → PENDING_EXTRA
     * - Giảm ngày → ghi chú hoàn tiền
     */
    @PostMapping("/{rentalId}/dates")
    public ResponseEntity<?> updateRentalDates(
            @PathVariable("rentalId") String rentalId,
            @RequestBody Map<String, String> body) {
        
        String username = getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy đơn thuê");
        }
        
        if (!Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền chỉnh sửa đơn này");
        }
        
        // Kiểm tra trạng thái - Chỉ cho phép update khi chưa bắt đầu
        String status = record.getStatus() != null ? record.getStatus().toUpperCase() : "";
        if (List.of("CANCELLED", "EXPIRED", "COMPLETED", "RETURNED", "IN_PROGRESS").contains(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể chỉnh sửa ngày thuê khi chuyến đã " + status.toLowerCase());
        }
        
        // Kiểm tra đã check-in chưa
        if (record.getCheckinPhotoData() != null || record.getCheckinTime() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể chỉnh sửa ngày thuê khi đã check-in");
        }
        
        // Parse ngày mới
        String startDateStr = body.get("startDate");
        String endDateStr = body.get("endDate");
        
        if (startDateStr == null || startDateStr.isBlank() || 
            endDateStr == null || endDateStr.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Vui lòng cung cấp đủ startDate và endDate");
        }
        
        LocalDate newStartDate;
        LocalDate newEndDate;
        try {
            newStartDate = LocalDate.parse(startDateStr);
            newEndDate = LocalDate.parse(endDateStr);
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Định dạng ngày không hợp lệ. Vui lòng dùng YYYY-MM-DD");
        }
        
        // Validation dates
        LocalDate today = LocalDate.now();
        if (newStartDate.isBefore(today)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày bắt đầu phải từ hôm nay trở đi");
        }
        
        if (newEndDate.isBefore(newStartDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        
        // Tính số ngày thuê mới
        long daySpan = ChronoUnit.DAYS.between(newStartDate, newEndDate) + 1;
        int newRentalDays = (int) Math.max(1, daySpan);
        
        // Lấy giá xe để tính lại tổng tiền
        Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không tìm thấy thông tin xe");
        }
        
        // Lưu lại giá cũ để so sánh
        double oldTotal = record.getTotal();
        int oldRentalDays = record.getRentalDays();
        
        double newTotal = vehicle.getPrice() * newRentalDays;
        double newDepositRequired = Math.round(newTotal * 0.3 * 100.0) / 100.0;
        
        // Lấy thông tin thanh toán hiện tại
        String paymentStatus = record.getPaymentStatus() != null ? record.getPaymentStatus().toUpperCase() : "";
        String paymentMethod = record.getPaymentMethod() != null ? record.getPaymentMethod().toLowerCase() : "";
        double depositPaid = record.getDepositPaidAmount() != null ? record.getDepositPaidAmount() : 0;
        double paidAmount = "PAID".equals(paymentStatus) ? oldTotal : depositPaid;
        
        // Kiểm tra nếu có phí phát sinh từ lần update trước
        boolean hadExtraFee = record.getAdditionalFeeAmount() != null && record.getAdditionalFeeAmount() > 0;
        double oldExtraFee = hadExtraFee ? record.getAdditionalFeeAmount() : 0;
        
        // Update record with new dates
        record.setStartDate(newStartDate);
        record.setEndDate(newEndDate);
        record.setRentalDays(newRentalDays);
        record.setTotal(newTotal);
        record.setDepositRequiredAmount(newDepositRequired);
        
        // Update startTime và endTime
        LocalDateTime newStartTime = newStartDate.atStartOfDay();
        LocalDateTime newEndTime = newEndDate.atTime(23, 59, 59);
        record.setStartTime(newStartTime);
        record.setEndTime(newEndTime);
        
        // Message để trả về cho user
        String message = "Đã cập nhật ngày thuê thành công";
        
        // Logic xử lý reset khi có phí phát sinh từ trước và đổi về số tiền <= đã thanh toán
        if (hadExtraFee && "PENDING_EXTRA".equals(paymentStatus) && newTotal <= depositPaid) {
            // User đã đổi về ngày ít hơn hoặc bằng ban đầu - XÓA phí phát sinh
            record.setAdditionalFeeAmount(null);
            record.setAdditionalFeePaidAmount(null);
            record.setAdditionalFeeNote(null);
            
            // Reset paymentStatus về trạng thái phù hợp
            if (depositPaid >= newTotal) {
                // Đã thanh toán đủ cho số ngày mới
                record.setPaymentStatus("PAID");
                record.setStatus("PAID");
                record.setHoldExpiresAt(newEndTime);
                message = "Đã cập nhật lại ngày thuê. Phí phát sinh đã được xóa.";
            } else if ("cash".equals(paymentMethod) && depositPaid >= newDepositRequired) {
                // Đủ cọc cho số ngày mới
                record.setPaymentStatus("PAY_AT_STATION");
                record.setStatus("PENDING_PAYMENT");
                message = "Đã cập nhật lại ngày thuê. Phí phát sinh đã được xóa. Vui lòng thanh toán phần còn lại tại trạm.";
            } else if ("cash".equals(paymentMethod)) {
                // Vẫn thiếu cọc
                record.setPaymentStatus("DEPOSIT_PENDING");
                record.setStatus("PENDING_PAYMENT");
                record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
                message = String.format("Đã cập nhật ngày thuê. Vui lòng chuyển thêm %,.0fđ tiền cọc.", 
                    newDepositRequired - depositPaid);
            }
            
            // Lưu và return sớm
            rentalRepo.save(record);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", record.getId());
            response.put("startDate", record.getStartDate());
            response.put("endDate", record.getEndDate());
            response.put("rentalDays", record.getRentalDays());
            response.put("total", record.getTotal());
            response.put("depositRequiredAmount", record.getDepositRequiredAmount());
            response.put("depositPaidAmount", record.getDepositPaidAmount());
            response.put("additionalFeeAmount", record.getAdditionalFeeAmount());
            response.put("additionalFeeNote", record.getAdditionalFeeNote());
            response.put("paymentStatus", record.getPaymentStatus());
            response.put("status", record.getStatus());
            response.put("message", message);
            
            return ResponseEntity.ok(response);
        }
        
        // Logic cập nhật phí phát sinh khi giảm nhưng vẫn còn phí (newTotal > depositPaid)
        if (hadExtraFee && "PENDING_EXTRA".equals(paymentStatus) && newTotal > depositPaid && newTotal < oldTotal) {
            double newExtraFee = newTotal - depositPaid;
            
            if (newExtraFee < oldExtraFee) {
                // Giảm phí phát sinh
                record.setAdditionalFeeAmount(newExtraFee);
                record.setAdditionalFeePaidAmount(null);
                record.setAdditionalFeeNote(String.format(
                    "Phí phát sinh do thay đổi ngày thuê.\n" +
                    "Tổng tiền mới: %,.0fđ\n" +
                    "Đã thanh toán: %,.0fđ\n" +
                    "Cần thanh toán thêm: %,.0fđ",
                    newTotal, depositPaid, newExtraFee
                ));
                message = String.format("Đã cập nhật ngày thuê. Phí phát sinh giảm xuống còn %,.0fđ", newExtraFee);
                
                // Lưu và return
                rentalRepo.save(record);
                
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("id", record.getId());
                response.put("startDate", record.getStartDate());
                response.put("endDate", record.getEndDate());
                response.put("rentalDays", record.getRentalDays());
                response.put("total", record.getTotal());
                response.put("depositRequiredAmount", record.getDepositRequiredAmount());
                response.put("additionalFeeAmount", record.getAdditionalFeeAmount());
                response.put("additionalFeeNote", record.getAdditionalFeeNote());
                response.put("paymentStatus", record.getPaymentStatus());
                response.put("status", record.getStatus());
                response.put("message", message);
                
                return ResponseEntity.ok(response);
            }
        }
        
        // Smart payment logic
        if (newTotal > oldTotal) {
            // TĂNG NGÀY THUÊ
            double difference = newTotal - oldTotal;
            
            if ("PAID".equals(paymentStatus)) {
                // Case A: Đã thanh toán 100% cho số ngày cũ → cần thanh toán thêm
                record.setAdditionalFeeAmount(difference);
                record.setAdditionalFeeNote("Phí phát sinh do tăng từ " + oldRentalDays + " ngày lên " + newRentalDays + " ngày");
                record.setPaymentStatus("PENDING_EXTRA");
                record.setStatus("PENDING_PAYMENT");
                record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(30));
                message = String.format("Cần thanh toán thêm %,.0fđ do tăng số ngày thuê", difference);
            } else if ("cash".equals(paymentMethod)) {
                // Case B: Đặt cọc tiền mặt
                if (depositPaid < newDepositRequired) {
                    // Cọc hiện tại < 30% tổng mới → cần cọc thêm
                    double additionalDeposit = newDepositRequired - depositPaid;
                    record.setPaymentStatus("DEPOSIT_PENDING");
                    record.setStatus("PENDING_PAYMENT");
                    record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(30));
                    message = String.format("Cần chuyển thêm %,.0fđ tiền cọc (30%% của tổng %,.0fđ)", additionalDeposit, newTotal);
                } else {
                    // Đã đủ cọc → PAY_AT_STATION
                    record.setPaymentStatus("PAY_AT_STATION");
                    message = "Đã cập nhật ngày thuê. Bạn đã đủ tiền cọc, thanh toán phần còn lại tại trạm.";
                }
            } else if ("bank_transfer".equals(paymentMethod)) {
                // Case C: Chuyển khoản nhưng chưa đủ
                if (paidAmount < newTotal) {
                    double additionalAmount = newTotal - paidAmount;
                    record.setAdditionalFeeAmount(additionalAmount);
                    record.setAdditionalFeeNote("Phí phát sinh do tăng từ " + oldRentalDays + " ngày lên " + newRentalDays + " ngày");
                    record.setPaymentStatus("PENDING_EXTRA");
                    record.setStatus("PENDING_PAYMENT");
                    record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(30));
                    message = String.format("Cần thanh toán thêm %,.0fđ", additionalAmount);
                }
            } else {
                // Chưa chọn phương thức thanh toán
                record.setPaymentStatus("PENDING");
                record.setStatus("PENDING_PAYMENT");
                record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
            }
        } else if (newTotal < oldTotal) {
            // GIẢM NGÀY THUÊ
            double refundAmount = oldTotal - newTotal;
            
            if ("PAID".equals(paymentStatus)) {
                // Đã thanh toán 100% → ghi chú hoàn tiền, cập nhật về PAID
                record.setAdditionalFeeNote(String.format("Hoàn lại %,.0fđ do giảm từ %d ngày xuống %d ngày. Vui lòng liên hệ bộ phận hỗ trợ để nhận tiền hoàn.", 
                    refundAmount, oldRentalDays, newRentalDays));
                record.setPaymentStatus("PAID");
                message = String.format("Đã cập nhật ngày thuê. Bạn sẽ được hoàn lại %,.0fđ. Vui lòng liên hệ bộ phận hỗ trợ.", refundAmount);
            } else if (depositPaid > newDepositRequired) {
                // Cọc đã đủ cho số ngày mới
                if ("cash".equals(paymentMethod)) {
                    record.setPaymentStatus("PAY_AT_STATION");
                    message = "Đã cập nhật ngày thuê. Tiền cọc đã đủ cho số ngày mới.";
                }
            }
            // Clear additional fee if any
            record.setAdditionalFeeAmount(null);
        } else {
            // KHÔNG ĐỔI TỔNG TIỀN (chỉ đổi ngày)
            message = "Đã cập nhật ngày thuê thành công";
        }
        
        rentalRepo.save(record);
        
        // Trả về record đã update
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", record.getId());
        response.put("startDate", record.getStartDate());
        response.put("endDate", record.getEndDate());
        response.put("rentalDays", record.getRentalDays());
        response.put("total", record.getTotal());
        response.put("depositRequiredAmount", record.getDepositRequiredAmount());
        response.put("additionalFeeAmount", record.getAdditionalFeeAmount());
        response.put("additionalFeeNote", record.getAdditionalFeeNote());
        response.put("paymentStatus", record.getPaymentStatus());
        response.put("status", record.getStatus());
        response.put("message", message);
        
        return ResponseEntity.ok(response);
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

        RentalRecord record;
        try {
            record = rentalRecordService.checkIn(rentalId, username, notes != null ? notes : "", photoData, latitude, longitude);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        
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
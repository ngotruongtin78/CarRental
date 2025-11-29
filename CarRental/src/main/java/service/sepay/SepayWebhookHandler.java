package CarRental.example.service.sepay;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.VehicleService;
import CarRental.example.service.sepay.SepayWebhookData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SepayWebhookHandler {

    private final RentalRecordRepository rentalRepo;
    private final VehicleService vehicleService;

    public SepayWebhookHandler(RentalRecordRepository rentalRepo, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleService = vehicleService;
    }

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookHandler.class);

    public ResponseEntity<String> processWebhook(SepayWebhookData data) {

        log.info("Webhook SePay nhận được: {}", data);

        String raw = data.getDescription();
        if (raw == null || raw.isBlank()) raw = data.getContent();

        if (raw == null || raw.isBlank()) {
            log.warn("Webhook rỗng cả description lẫn content");
            return ResponseEntity.ok("NO_DESCRIPTION");
        }

        String lower = raw.toLowerCase();
        String rentalId = null;
        boolean depositFlow = false;
        boolean incidentFlow = false;

        java.util.regex.Matcher depositMatcher = java.util.regex.Pattern
                .compile("depositrental(\\d+)")
                .matcher(lower);

        if (depositMatcher.find()) {
            rentalId = "rental" + depositMatcher.group(1);
            depositFlow = true;
        }

        java.util.regex.Matcher incidentMatcher = java.util.regex.Pattern
                .compile("incidentrental(\\d+)")
                .matcher(lower);

        if (incidentMatcher.find()) {
            rentalId = "rental" + incidentMatcher.group(1);
            incidentFlow = true;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("rental(\\d+)")
                .matcher(lower);

        if (rentalId == null && matcher.find()) {
            rentalId = matcher.group(0);
        }

        if ((rentalId == null || rentalId.isEmpty()) && lower.contains("carrental_")) {
            String digits = lower.substring(lower.indexOf("carrental_") + "carrental_".length())
                    .replaceAll("[^0-9]", "")
                    .trim();
            if (!digits.isEmpty()) {
                rentalId = "rental" + digits;
            }
        }

        if (rentalId == null || rentalId.isEmpty()) {
            log.warn("Không tìm thấy rentalId trong nội dung: {}", raw);
            return ResponseEntity.ok("NO_RENTAL_ID");
        }

        if (depositFlow) {
            log.info("Nhận giao dịch đặt cọc cho {}", rentalId);
        }
        if (incidentFlow) {
            log.info("Nhận giao dịch phí phát sinh cho {}", rentalId);
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            log.warn("Không tìm thấy đơn với id: {}", rentalId);
            return ResponseEntity.ok("RENTAL_NOT_FOUND");
        }

        boolean cashFlow = "cash".equalsIgnoreCase(record.getPaymentMethod());
        double depositRequired = record.getDepositRequiredAmount() != null
                ? record.getDepositRequiredAmount()
                : (cashFlow ? Math.round(record.getTotal() * 0.3 * 100.0) / 100.0 : 0.0);
        Double depositPaid = record.getDepositPaidAmount();
        double currentDeposit = depositPaid != null ? depositPaid : 0.0;

        double depositRemaining = Math.max(0, depositRequired - currentDeposit);

        if (!depositFlow && !incidentFlow && cashFlow && depositRemaining > 0) {
            depositFlow = true;
            log.info("Suy luận webhook là giao dịch đặt cọc cho {} vì đơn tiền mặt còn thiếu cọc", rentalId);
        }

        if (depositFlow) {
            log.info("Nhận giao dịch đặt cọc cho {}", rentalId);
        }
        if (incidentFlow) {
            log.info("Nhận giao dịch phí phát sinh cho {}", rentalId);
        }

        double incomingAmount = 0;
        try {
            incomingAmount = Double.parseDouble(data.getAmount());
        } catch (Exception ignored) {}

        if (incomingAmount <= 0) {
            try {
                incomingAmount = Double.parseDouble(data.getSub_amount());
            } catch (Exception ignored) {}
        }

        if (incidentFlow) {
            double recordedFee = record.getAdditionalFeeAmount() != null
                    ? record.getAdditionalFeeAmount()
                    : record.getDamageFee();
            double paidFee = record.getAdditionalFeePaidAmount() != null
                    ? record.getAdditionalFeePaidAmount()
                    : 0.0;

            if (incomingAmount <= 0) {
                double remaining = Math.max(0, recordedFee - paidFee);
                incomingAmount = remaining > 0 ? remaining : recordedFee;
            }

            if (incomingAmount <= 0) {
                log.warn("Webhook {} không có số tiền hợp lệ (amount={}, sub_amount={})", rentalId, data.getAmount(), data.getSub_amount());
                return ResponseEntity.ok("INVALID_AMOUNT");
            }

            double newPaid = paidFee + incomingAmount;
            record.setAdditionalFeeAmount(recordedFee);
            record.setAdditionalFeePaidAmount(newPaid);
            record.setAdditionalFeePaidAt(java.time.LocalDateTime.now());
            rentalRepo.save(record);
            log.info("Đơn {} đã ghi nhận thanh toán phí phát sinh {}", rentalId, incomingAmount);
            return ResponseEntity.ok("OK");
        }

        if (incomingAmount <= 0 && depositFlow) {
            incomingAmount = depositRemaining > 0 ? depositRemaining : depositRequired;
            record.setDepositRequiredAmount(depositRequired);
        }

        if (incomingAmount <= 0 && !depositFlow && !incidentFlow) {
            double outstanding = Math.max(0, record.getTotal() - currentDeposit);
            incomingAmount = outstanding;
        }

        if (incomingAmount <= 0) {
            log.warn("Webhook {} không có số tiền hợp lệ (amount={}, sub_amount={})", rentalId, data.getAmount(), data.getSub_amount());
            return ResponseEntity.ok("INVALID_AMOUNT");
        }

        if (depositFlow && cashFlow) {
            double newPaid = currentDeposit + incomingAmount;
            record.setDepositPaidAmount(newPaid);
            record.setDepositPaidAt(java.time.LocalDateTime.now());
            record.setWalletReference(data.getTranId());
            record.setDepositRequiredAmount(depositRequired);

            // ===== LOGIC GIỮ CHỖ THÔNG MINH =====
            java.time.LocalDateTime holdExpiry;
            double totalAmount = record.getTotal();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            if (newPaid >= totalAmount) {
                // Chuyển khoản 100% qua tiền mặt - Giữ đến hết ngày thuê
                record.setPaymentStatus("PAID");
                record.setStatus("PAID");
                record.setPaidAt(now);
                
                java.time.LocalDateTime endTime = record.getEndTime();
                if (endTime == null) {
                    java.time.LocalDate endDate = record.getEndDate();
                    java.time.LocalDate startDate = record.getStartDate();
                    
                    if (endDate != null) {
                        // Lấy giờ từ startTime nếu có
                        if (record.getStartTime() != null) {
                            int hour = record.getStartTime().getHour();
                            int minute = record.getStartTime().getMinute();
                            endTime = endDate.atTime(hour, minute);
                        } else {
                            endTime = endDate.atTime(23, 59, 59);
                        }
                    } else {
                        endTime = now.plusHours(24);
                    }
                }
                
                holdExpiry = endTime;
                record.setHoldExpiresAt(holdExpiry);
                rentalRepo.save(record);
                
                try {
                    vehicleService.markRented(record.getVehicleId(), rentalId);
                } catch (Exception e) {
                    log.error("Lỗi cập nhật xe: {}", e.getMessage());
                }
                
                log.info("Đơn {} đã thanh toán 100% qua tiền mặt, giữ đến {}", rentalId, holdExpiry);
                return ResponseEntity.ok("OK");
            }

            if (newPaid >= depositRequired) {
                // Đặt cọc đủ 30% - Giữ đến startTime (đầu ngày thuê)
                record.setPaymentStatus("PAY_AT_STATION");
                record.setStatus("PENDING_PAYMENT");
                
                // Giữ đến đầu ngày thuê
                java.time.LocalDateTime startTime = record.getStartTime();
                if (startTime == null && record.getStartDate() != null) {
                    startTime = record.getStartDate().atStartOfDay();
                }
                if (startTime == null) {
                    startTime = now.plusHours(8);
                }
                
                holdExpiry = startTime;
                record.setHoldExpiresAt(holdExpiry);
                rentalRepo.save(record);
                
                try {
                    vehicleService.markDeposited(record.getVehicleId(), rentalId);
                } catch (Exception e) {
                    log.error("Lỗi cập nhật xe sau đặt cọc: {}", e.getMessage());
                }
                
                log.info("Đơn {} đã đặt cọc {}/{}, giữ đến startTime {}", 
                         rentalId, newPaid, depositRequired, holdExpiry);
                return ResponseEntity.ok("OK");
            }

            // Chưa đủ cọc - Giữ 8 tiếng (cho phép user tiếp tục chuyển)
            record.setPaymentStatus("DEPOSIT_PENDING");
            record.setStatus("PENDING_PAYMENT");
            holdExpiry = java.time.LocalDateTime.now().plusHours(8);
            record.setHoldExpiresAt(holdExpiry);
            rentalRepo.save(record);
            log.warn("CẢNH BÁO: Đơn {} chưa đủ cọc {}/{}, giữ 8 tiếng", 
                     rentalId, newPaid, depositRequired);
            return ResponseEntity.ok("OK");
        }

        if ("PAID".equalsIgnoreCase(record.getPaymentStatus())) {
            log.info("Đơn {} đã thanh toán trước đó -> bỏ qua webhook", rentalId);
            return ResponseEntity.ok("ALREADY_PAID");
        }

        // Chuyển khoản 100% - Giữ đến hết ngày thuê
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        record.setPaymentStatus("PAID");
        record.setStatus("PAID");
        record.setPaidAt(now);
        record.setWalletReference(data.getTranId());
        record.setDepositPaidAmount(record.getTotal());
        record.setDepositPaidAt(now);

        java.time.LocalDateTime endTime = record.getEndTime();
        if (endTime == null) {
            java.time.LocalDate endDate = record.getEndDate();
            if (endDate != null) {
                // Lấy giờ từ startTime nếu có
                if (record.getStartTime() != null) {
                    int hour = record.getStartTime().getHour();
                    int minute = record.getStartTime().getMinute();
                    endTime = endDate.atTime(hour, minute);
                } else {
                    endTime = endDate.atTime(23, 59, 59);
                }
            } else {
                endTime = now.plusDays(1).with(java.time.LocalTime.of(23, 59, 59));
            }
        }

        java.time.LocalDateTime holdExpiry = endTime;
        record.setHoldExpiresAt(holdExpiry);
        rentalRepo.save(record);

        try {
            vehicleService.markRented(record.getVehicleId(), rentalId);
        } catch (Exception e) {
            log.error("Lỗi cập nhật xe: {}", e.getMessage());
        }

        log.info("Đơn {} thanh toán chuyển khoản 100%, giữ đến {}", rentalId, holdExpiry);
        return ResponseEntity.ok("OK");
    }
}

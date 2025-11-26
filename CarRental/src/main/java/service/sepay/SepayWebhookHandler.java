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

            if (newPaid >= record.getTotal()) {
                record.setPaymentStatus("PAID");
                record.setStatus("PAID");
                record.setPaidAt(java.time.LocalDateTime.now());
                record.setHoldExpiresAt(null);
                rentalRepo.save(record);
                try {
                    vehicleService.markRented(record.getVehicleId(), rentalId);
                } catch (Exception e) {
                    log.error("Lỗi cập nhật xe: {}", e.getMessage());
                }
                log.info("Đơn {} đã thanh toán đủ qua chuyển khoản", rentalId);
                return ResponseEntity.ok("OK");
            }

            if (newPaid >= depositRequired) {
                record.setPaymentStatus("PAY_AT_STATION");
                record.setStatus("ACTIVE");
                java.time.LocalDate holdStart = record.getStartDate() != null
                        ? record.getStartDate()
                        : java.time.LocalDate.now();
                java.time.LocalDateTime holdUntil = holdStart.atStartOfDay().plusDays(1);
                if (holdUntil.isBefore(java.time.LocalDateTime.now())) {
                    holdUntil = java.time.LocalDateTime.now().plusDays(1);
                }
                record.setHoldExpiresAt(holdUntil);
                rentalRepo.save(record);
                try {
                    vehicleService.markDeposited(record.getVehicleId(), rentalId);
                } catch (Exception e) {
                    log.error("Lỗi cập nhật xe sau đặt cọc: {}", e.getMessage());
                }
                log.info("Đơn {} đã đặt cọc đủ, chuyển sang PAY_AT_STATION", rentalId);
                return ResponseEntity.ok("OK");
            }

            record.setPaymentStatus("DEPOSIT_PENDING");
            rentalRepo.save(record);
            log.info("Đơn {} đã ghi nhận đặt cọc {} qua chuyển khoản", rentalId, incomingAmount);
            return ResponseEntity.ok("OK");
        }

        if ("PAID".equalsIgnoreCase(record.getPaymentStatus())) {
            log.info("Đơn {} đã thanh toán trước đó -> bỏ qua webhook", rentalId);
            return ResponseEntity.ok("ALREADY_PAID");
        }

        record.setPaymentStatus("PAID");
        record.setStatus("PAID");
        record.setPaidAt(java.time.LocalDateTime.now());
        record.setWalletReference(data.getTranId());
        rentalRepo.save(record);

        try {
            vehicleService.markRented(record.getVehicleId(), rentalId);
        } catch (Exception e) {
            log.error("Lỗi cập nhật xe: {}", e.getMessage());
        }

        log.info("Đơn {} đã được cập nhật PAID!", rentalId);
        return ResponseEntity.ok("OK");
    }
}

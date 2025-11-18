package CarRental.example.service.sepay;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.VehicleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class SepayWebhookHandler {

    private final RentalRecordRepository rentalRepo;
    private final VehicleService vehicleService;

    @Value("${sepay.secret-key:}")
    private String secretKey;

    public SepayWebhookHandler(RentalRecordRepository rentalRepo, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleService = vehicleService;
    }

    public ResponseEntity<String> processWebhook(SepayWebhookData data) {
        if (data == null || !StringUtils.hasText(data.getOrderId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Thiếu orderId trong dữ liệu webhook");
        }
        if (!StringUtils.hasText(secretKey)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Chưa cấu hình sepay.secret-key");
        }
        if (!SepaySignatureUtil.verifySignature(data, secretKey)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Chữ ký webhook không hợp lệ");
        }
        if (!"SUCCESS".equalsIgnoreCase(data.getStatus())) {
            return ResponseEntity.ok("Bỏ qua giao dịch không thành công");
        }

        RentalRecord record = rentalRepo.findById(data.getOrderId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Không tìm thấy chuyến thuê");
        }

        if (expireRentalIfNeeded(record)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("Đơn đặt đã hết hạn thanh toán");
        }

        record.setPaymentStatus("PAID");
        record.setPaymentMethod("bank_transfer");
        record.setStatus("PAID");
        record.setHoldExpiresAt(null);
        record.setPaidAt(LocalDateTime.now());
        if (data.getAmount() > 0) {
            record.setTotal((double) data.getAmount());
        }

        rentalRepo.save(record);
        vehicleService.markRented(record.getVehicleId(), record.getId());

        return ResponseEntity.ok("OK");
    }

    private boolean expireRentalIfNeeded(RentalRecord record) {
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
}

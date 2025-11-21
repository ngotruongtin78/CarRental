package CarRental.example.service.sepay;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookHandler {

    private final RentalRecordRepository rentalRepo;
    private final VehicleService vehicleService;

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

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("rental(\\d+)")
                .matcher(lower);

        if (matcher.find()) {
            rentalId = matcher.group(0);
            log.info("===> rentalId lấy theo regex rentalXX: {}", rentalId);
        }

        if ((rentalId == null || rentalId.isEmpty()) && lower.contains("carrental_")) {
            String digits = lower.substring(lower.indexOf("carrental_") + "carrental_".length())
                    .replaceAll("[^0-9]", "")
                    .trim();
            if (!digits.isEmpty()) {
                rentalId = "rental" + digits;
            }
            log.info("===> rentalId từ CARRENTAL_XX: {}", rentalId);
        }

        if (rentalId == null || rentalId.isEmpty()) {
            log.warn("Không tìm thấy rentalId trong nội dung: {}", raw);
            return ResponseEntity.ok("NO_RENTAL_ID");
        }

        log.info("===> rentalId chuẩn: {}", rentalId);

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            log.warn("Không tìm thấy đơn với id: {}", rentalId);
            return ResponseEntity.ok("RENTAL_NOT_FOUND");
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

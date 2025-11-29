package CarRental.example.scheduler;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class RentalExpirationScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(RentalExpirationScheduler.class);
    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;
    
    public RentalExpirationScheduler(RentalRecordRepository rentalRepo, VehicleRepository vehicleRepo) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
    }
    
    @Scheduled(fixedRate = 300000) // 5 phút
    public void checkExpiredRentals() {
        LocalDateTime now = LocalDateTime.now();
        
        // Use database query for better performance instead of findAll().filter()
        List<RentalRecord> expiredRentals = rentalRepo.findExpiredRentalsNotCheckedIn(now);
        
        expiredRentals.forEach(this::expireRental);
        if (!expiredRentals.isEmpty()) {
            log.info("✅ Đã xử lý {} đơn hết hạn", expiredRentals.size());
        }
    }
    
    private void expireRental(RentalRecord record) {
        try {
            String paymentMethod = record.getPaymentMethod() != null ? record.getPaymentMethod().toLowerCase() : "";
            double depositPaid = record.getDepositPaidAmount() != null ? record.getDepositPaidAmount() : 0;
            double totalAmount = record.getTotal();
            
            String refundNote;
            
            if (depositPaid == 0) {
                // ===== Case 1: Chưa thanh toán gì =====
                refundNote = "Đơn đã bị hủy do không thanh toán trong thời gian quy định.";
                log.info("📝 Đơn {} hủy - Chưa thanh toán", record.getId());
                
            } else if ("cash".equals(paymentMethod)) {
                // ===== Case 2: TIỀN MẶT - Đã đặt cọc =====
                // KHÔNG HOÀN TIỀN (giữ 100% tiền cọc)
                refundNote = String.format(
                    "⚠️ KHÔNG HOÀN TIỀN ĐẶT CỌC\n\n" +
                    "Tiền đặt cọc: %,.0fđ sẽ KHÔNG được hoàn lại do bạn không đến nhận xe trong thời gian quy định.\n\n" +
                    "Khi chọn phương thức 'Thanh toán tiền mặt tại trạm', bạn đã cam kết:\n" +
                    "• Chuyển khoản đặt cọc 30%% để giữ xe\n" +
                    "• Đến trạm đúng giờ để nhận xe và thanh toán phần còn lại\n\n" +
                    "Việc không đến nhận xe được xem là vi phạm cam kết.\n\n" +
                    "Nếu có vấn đề phát sinh, vui lòng liên hệ bộ phận hỗ trợ trong vòng 24h kể từ khi nhận thông báo này.",
                    depositPaid
                );
                log.warn("❌ Đơn {} - TIỀN MẶT: Giữ toàn bộ {}đ, KHÔNG hoàn", record.getId(), depositPaid);
                
            } else if ("bank_transfer".equals(paymentMethod) && depositPaid >= totalAmount) {
                // ===== Case 3: CHUYỂN KHOẢN - Đã thanh toán 100% =====
                // PHẠT 30% (= số tiền đặt cọc)
                double penalty = Math.round(totalAmount * 0.3 * 100.0) / 100.0;
                double refund = totalAmount - penalty;
                
                refundNote = String.format(
                    "📋 CHÍNH SÁCH HỦY MUỘN\n\n" +
                    "Tổng tiền đã thanh toán: %,.0fđ\n" +
                    "Phí phạt không đến nhận xe: %,.0fđ (30%% tổng tiền)\n" +
                    "Số tiền được hoàn lại: %,.0fđ (70%% tổng tiền)\n\n" +
                    "Theo chính sách của chúng tôi:\n" +
                    "• Hủy trước 24h: Hoàn 100%%\n" +
                    "• Không đến nhận xe: Giữ phí đặt cọc (30%%)\n\n" +
                    "Để nhận lại số tiền %,.0fđ, vui lòng:\n" +
                    "1. Vào mục 'Hỗ trợ' trong Hồ sơ cá nhân\n" +
                    "2. Gửi yêu cầu hoàn tiền với mã đơn: %s\n" +
                    "3. Cung cấp thông tin tài khoản nhận hoàn tiền\n\n" +
                    "Thời gian xử lý: 3-5 ngày làm việc.",
                    totalAmount, penalty, refund, refund, record.getId()
                );
                log.warn("⚠️ Đơn {} - CHUYỂN KHOẢN: Phạt {}đ (30%), hoàn {}đ", 
                         record.getId(), penalty, refund);
                
            } else {
                // ===== Edge case =====
                refundNote = "Đơn đã hết hạn. Vui lòng liên hệ bộ phận hỗ trợ để được tư vấn chi tiết.";
                log.warn("⚠️ Đơn {} hết hạn - Edge case: method={}, paid={}", 
                         record.getId(), paymentMethod, depositPaid);
            }
            
            // Cập nhật trạng thái đơn
            record.setStatus("EXPIRED");
            record.setPaymentStatus("NO_SHOW");
            record.setAdditionalFeeNote(refundNote);
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            
            // Giải phóng xe
            Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setAvailable(true);
                vehicle.setBookingStatus("AVAILABLE");
                vehicle.setPendingRentalId(null);
                vehicleRepo.save(vehicle);
                log.info("✅ Xe {} đã được giải phóng", vehicle.getPlate());
            }
            
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý đơn hết hạn {}: {}", record.getId(), e.getMessage());
        }
    }
}

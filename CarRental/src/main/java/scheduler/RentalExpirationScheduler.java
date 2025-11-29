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
            double depositPaid = record.getDepositPaidAmount() != null ? record.getDepositPaidAmount() : 0;
            double totalPaid = record.getTotal();
            String refundNote;
            
            if (depositPaid == 0) {
                refundNote = "Đơn đã bị hủy do không thanh toán trong thời gian quy định.";
            } else if (depositPaid < totalPaid) {
                double penalty = Math.round(depositPaid * 0.2 * 100.0) / 100.0;
                double refund = depositPaid - penalty;
                refundNote = String.format(
                    "Đã giữ phí phạt: %,.0fđ (20%% đặt cọc).\nSố tiền hoàn lại: %,.0fđ.\nVui lòng vào mục 'Hỗ trợ' trong Hồ sơ cá nhân để gửi yêu cầu hoàn tiền.",
                    penalty, refund
                );
            } else {
                double penalty = Math.round(totalPaid * 0.2 * 100.0) / 100.0;
                double refund = totalPaid - penalty;
                refundNote = String.format(
                    "Đã giữ phí phạt: %,.0fđ (20%% tổng tiền).\nSố tiền hoàn lại: %,.0fđ.\nVui lòng vào mục 'Hỗ trợ' trong Hồ sơ cá nhân để gửi yêu cầu hoàn tiền.",
                    penalty, refund
                );
            }
            
            record.setStatus("EXPIRED");
            record.setPaymentStatus("NO_SHOW");
            record.setAdditionalFeeNote(refundNote);
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            
            Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setAvailable(true);
                vehicle.setBookingStatus("AVAILABLE");
                vehicle.setPendingRentalId(null);
                vehicleRepo.save(vehicle);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý đơn {}: {}", record.getId(), e.getMessage());
        }
    }
}

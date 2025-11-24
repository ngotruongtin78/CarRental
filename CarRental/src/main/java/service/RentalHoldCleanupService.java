package CarRental.example.service;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalHoldCleanupService {

    private final RentalRecordRepository rentalRecordRepository;
    private final VehicleService vehicleService;

    public RentalHoldCleanupService(RentalRecordRepository rentalRecordRepository, VehicleService vehicleService) {
        this.rentalRecordRepository = rentalRecordRepository;
        this.vehicleService = vehicleService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void releaseUnpaidSelections() {
        LocalDateTime now = LocalDateTime.now();
        List<RentalRecord> expired = rentalRecordRepository.findByStatusAndHoldExpiresAtBefore("PENDING_PAYMENT", now);

        for (RentalRecord record : expired) {
            boolean hasPaymentMethod = record.getPaymentMethod() != null && !record.getPaymentMethod().isBlank();
            if (hasPaymentMethod) continue;

            record.setStatus("CANCELLED");
            record.setPaymentStatus("EXPIRED");
            record.setHoldExpiresAt(null);
            rentalRecordRepository.save(record);
            vehicleService.releaseHold(record.getVehicleId(), record.getId());
        }
    }
}

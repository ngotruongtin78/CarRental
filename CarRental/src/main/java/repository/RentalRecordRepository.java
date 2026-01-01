package CarRental.example.repository;

import CarRental.example.document.RentalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RentalRecordRepository extends JpaRepository<RentalRecord, Long> {
    List<RentalRecord> findByUsername(String username);

    List<RentalRecord> findByStatusAndHoldExpiresAtBefore(String status, LocalDateTime time);

    /**
     * Find expired rentals that haven't checked in and are still in active status.
     * This query filters at database level for better performance.
     */
    @Query("SELECT r FROM RentalRecord r WHERE r.holdExpiresAt < ?1 AND r.holdExpiresAt IS NOT NULL " +
           "AND r.checkinPhotoData IS NULL AND r.checkinTime IS NULL " +
           "AND r.status NOT IN ('CANCELLED', 'EXPIRED', 'COMPLETED', 'RETURNED', 'IN_PROGRESS') " +
           "AND r.paymentStatus NOT IN ('PAID')")
    List<RentalRecord> findExpiredRentalsNotCheckedIn(LocalDateTime now);
}

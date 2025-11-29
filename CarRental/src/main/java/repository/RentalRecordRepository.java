package CarRental.example.repository;

import CarRental.example.document.RentalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RentalRecordRepository extends MongoRepository<RentalRecord, String> {
    List<RentalRecord> findByUsername(String username);

    List<RentalRecord> findByStatusAndHoldExpiresAtBefore(String status, LocalDateTime time);

    /**
     * Find expired rentals that haven't checked in and are still in active status.
     * This query filters at database level for better performance.
     */
    @Query("{ 'holdExpiresAt': { $lt: ?0, $ne: null }, " +
           "'checkinPhotoData': null, " +
           "'checkinTime': null, " +
           "'status': { $nin: ['CANCELLED', 'EXPIRED', 'COMPLETED', 'RETURNED', 'IN_PROGRESS'] } }")
    List<RentalRecord> findExpiredRentalsNotCheckedIn(LocalDateTime now);
}

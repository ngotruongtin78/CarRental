package CarRental.example.repository;

import CarRental.example.document.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(String bookingId);
    List<Review> findByCarId(String carId);
    List<Review> findByStaffId(String staffId);
    List<Review> findByUserId(String userId);
    boolean existsByBookingId(String bookingId);
    List<Review> findAllByOrderByReviewDateDesc();
}

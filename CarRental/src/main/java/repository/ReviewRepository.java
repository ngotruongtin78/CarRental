package CarRental.example.repository;

import CarRental.example.document.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
    List<Review> findByCarId(Long carId);
    List<Review> findByStaffId(Long staffId);
    List<Review> findByUserId(Long userId);
    boolean existsByBookingId(Long bookingId);
    List<Review> findAllByOrderByReviewDateDesc();
}

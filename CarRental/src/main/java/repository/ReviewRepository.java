package CarRental.example.repository;

import CarRental.example.document.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {
    Optional<Review> findByBookingId(String bookingId);
    List<Review> findByCarId(String carId);
    List<Review> findByStaffId(String staffId);
    List<Review> findByUserId(String userId);
    boolean existsByBookingId(String bookingId);
    List<Review> findAllByOrderByReviewDateDesc();
}

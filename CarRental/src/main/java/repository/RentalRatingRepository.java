package CarRental.example.repository;

import CarRental.example.document.RentalRating;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RentalRatingRepository extends MongoRepository<RentalRating, String> {
    Optional<RentalRating> findByRentalId(String rentalId);
    List<RentalRating> findAllByUserId(String userId);
}

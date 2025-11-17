package CarRental.example.repository;

import CarRental.example.document.RentalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RentalRecordRepository extends MongoRepository<RentalRecord, String> {
    List<RentalRecord> findByUsername(String username);
}

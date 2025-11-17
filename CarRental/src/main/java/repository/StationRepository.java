package CarRental.example.repository;

import CarRental.example.document.Station;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StationRepository extends MongoRepository<Station, String> {
}

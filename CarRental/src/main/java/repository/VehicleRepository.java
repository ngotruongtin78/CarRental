package CarRental.example.repository;

import CarRental.example.document.Vehicle;
import CarRental.example.repository.VehicleRepositoryCustom;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository
        extends MongoRepository<Vehicle, String>, VehicleRepositoryCustom {

    long countByStationIdAndAvailable(String stationId, boolean available);
    List<Vehicle> findByStationIdAndAvailable(String stationId, boolean available);

    @Query("{ '_id': ?0 }")
    Vehicle findVehicleById(String id);
}

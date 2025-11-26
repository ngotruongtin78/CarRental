package CarRental.example.repository;

import CarRental.example.document.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository
        extends MongoRepository<Vehicle, String>, VehicleRepositoryCustom {

    long countByStationIdAndAvailable(String stationId, boolean available);
    List<Vehicle> findByStationId(String stationId);
    List<Vehicle> findByStationIdAndAvailable(String stationId, boolean available);
    List<Vehicle> findByStationIdAndBookingStatusNot(String stationId, String bookingStatus);

    // Hàm đếm cơ bản (Dùng cho RENTED, MAINTENANCE)
    long countByStationIdAndBookingStatus(String stationId, String bookingStatus);

    // [CẬP NHẬT QUAN TRỌNG] Hàm đếm xe Sẵn sàng "thông minh"
    // Logic: Đếm xe tại trạm đó MÀ (bookingStatus là 'AVAILABLE' HOẶC bookingStatus bị rỗng/null) VÀ (available = true)
    @Query(value = "{ 'stationId': ?0, $or: [ {'bookingStatus': 'AVAILABLE'}, {'bookingStatus': null}, {'bookingStatus': {$exists: false}} ], 'available': true }", count = true)
    long countAvailableVehiclesRobust(String stationId);

    @Query("{ '_id': ?0 }")
    Vehicle findVehicleById(String id);
}
package CarRental.example.repository;

import CarRental.example.document.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    long countByStationIdAndAvailable(Long stationId, boolean available);
    List<Vehicle> findByStationId(Long stationId);
    List<Vehicle> findByStationIdAndAvailable(Long stationId, boolean available);
    List<Vehicle> findByStationIdAndBookingStatusNot(Long stationId, String bookingStatus);

    // Hàm đếm cơ bản (Dùng cho RENTED, MAINTENANCE)
    long countByStationIdAndBookingStatus(Long stationId, String bookingStatus);

    // [CẬP NHẬT QUAN TRỌNG] Hàm đếm xe Sẵn sàng "thông minh"
    // Logic: Đếm xe tại trạm đó MÀ (bookingStatus là 'AVAILABLE' HOẶC bookingStatus bị rỗng/null) VÀ (available = true)
    @Query("SELECT COUNT(v) FROM Vehicle v WHERE v.stationId = ?1 AND (v.bookingStatus = 'AVAILABLE' OR v.bookingStatus IS NULL) AND v.available = true")
    long countAvailableVehiclesRobust(Long stationId);

    @Query("SELECT v FROM Vehicle v WHERE v.id = ?1")
    Vehicle findVehicleById(Long id);
}
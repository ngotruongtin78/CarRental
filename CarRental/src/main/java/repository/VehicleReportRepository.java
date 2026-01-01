package CarRental.example.repository;

import CarRental.example.document.VehicleReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleReportRepository extends JpaRepository<VehicleReport, Long> {

    List<VehicleReport> findByVehicleId(Long vehicleId);

    List<VehicleReport> findByStationId(Long stationId);

    List<VehicleReport> findByStaffId(Long staffId);

    List<VehicleReport> findByStatus(String status);

    List<VehicleReport> findByStationIdAndStatus(Long stationId, String status);
}


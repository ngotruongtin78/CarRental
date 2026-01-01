package CarRental.example.repository;

import CarRental.example.document.VehicleReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleReportRepository extends JpaRepository<VehicleReport, Long> {

    List<VehicleReport> findByVehicleId(String vehicleId);

    List<VehicleReport> findByStationId(String stationId);

    List<VehicleReport> findByStaffId(String staffId);

    List<VehicleReport> findByStatus(String status);

    List<VehicleReport> findByStationIdAndStatus(String stationId, String status);
}


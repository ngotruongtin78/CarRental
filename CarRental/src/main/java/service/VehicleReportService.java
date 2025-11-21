package CarRental.example.service;

import CarRental.example.document.VehicleReport;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.VehicleReportRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleReportService {

    @Autowired
    private VehicleReportRepository vehicleReportRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    public VehicleReport createReport(VehicleReport report) {
        return vehicleReportRepository.save(report);
    }

    public Optional<VehicleReport> getReportById(String id) {
        return vehicleReportRepository.findById(id);
    }

    public List<VehicleReport> getReportsByVehicleId(String vehicleId) {
        return vehicleReportRepository.findByVehicleId(vehicleId);
    }

    public List<VehicleReport> getReportsByStationId(String stationId) {
        return vehicleReportRepository.findByStationId(stationId);
    }

    public List<VehicleReport> getReportsByStaffId(String staffId) {
        return vehicleReportRepository.findByStaffId(staffId);
    }

    public List<VehicleReport> getReportsByStatus(String status) {
        return vehicleReportRepository.findByStatus(status);
    }

    public List<VehicleReport> getReportsByStationIdAndStatus(String stationId, String status) {
        return vehicleReportRepository.findByStationIdAndStatus(stationId, status);
    }

    public VehicleReport updateReportStatus(String reportId, String newStatus) {
        Optional<VehicleReport> reportOpt = vehicleReportRepository.findById(reportId);
        if (reportOpt.isPresent()) {
            VehicleReport report = reportOpt.get();
            report.setStatus(newStatus);
            VehicleReport updated = vehicleReportRepository.save(report);

            // If status is set to RESOLVED, update vehicle status to AVAILABLE
            if ("RESOLVED".equalsIgnoreCase(newStatus)) {
                Optional<Vehicle> vehicleOpt = vehicleRepository.findById(report.getVehicleId());
                if (vehicleOpt.isPresent()) {
                    Vehicle vehicle = vehicleOpt.get();
                    vehicle.setAvailable(true);
                    vehicle.setBookingStatus("AVAILABLE");
                    vehicle.setIssue(null);
                    vehicle.setIssueSeverity(null);
                    vehicleRepository.save(vehicle);
                }
            }

            return updated;
        }
        return null;
    }

    public VehicleReport addNoteToReport(String reportId, String notes) {
        Optional<VehicleReport> reportOpt = vehicleReportRepository.findById(reportId);
        if (reportOpt.isPresent()) {
            VehicleReport report = reportOpt.get();
            report.setNotes(notes);
            return vehicleReportRepository.save(report);
        }
        return null;
    }

    public List<VehicleReport> getAllReports() {
        return vehicleReportRepository.findAll();
    }

    public void deleteReport(String reportId) {
        vehicleReportRepository.deleteById(reportId);
    }
}


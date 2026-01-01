package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.document.Station;
import CarRental.example.document.VehicleReport;
import CarRental.example.document.Staff;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.repository.VehicleReportRepository;
import CarRental.example.repository.StaffRepository;
import CarRental.example.service.VehicleService;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Map;
import java.util.HashMap;


import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleRepository repo;

    @Autowired
    private RentalRecordRepository rentalRepo;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private VehicleReportRepository vehicleReportRepository;

    @Autowired
    private StaffRepository staffRepository;

    @GetMapping("/station/{stationId}")
    public List<Vehicle> getByStation(@PathVariable("stationId") Long stationId) {
        releaseExpiredHolds(stationId);
        return repo.findByStationIdAndAvailable(stationId, true)
                .stream()
                .filter(v -> !"RENTED".equalsIgnoreCase(v.getBookingStatus()))
                .toList();
    }

    @GetMapping("/station/{stationId}/staff-station")
    public ResponseEntity<?> getByStationWithInfo(@PathVariable("stationId") Long stationId) {
        try {
            // Lấy danh sách xe tại trạm (bao gồm cả xe đang thuê)
            List<Vehicle> vehicles = repo.findByStationId(stationId);

            // Lấy thông tin trạm
            Station station = stationRepository.findById(stationId).orElse(null);
            String stationName = (station != null) ? station.getName() : "Unknown Station";

            Map<String, Object> response = new HashMap<>();
            response.put("stationName", stationName);
            response.put("vehicles", vehicles);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }


    @GetMapping("/admin/all")
    public List<Vehicle> getAllVehicles() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Vehicle> getVehicle(@PathVariable("id") Long id) {
        return repo.findById(id);
    }

    @GetMapping("/admin/{id}")
    public Optional<Vehicle> getVehicleById(@PathVariable("id") Long id) {
        return repo.findById(id);
    }

    @PostMapping("/admin/add")
    public Vehicle addVehicle(@RequestBody Vehicle vehicle) {
        return repo.save(vehicle);
    }

    @PutMapping("/admin/update/{id}")
    public Vehicle updateVehicle(@PathVariable("id") Long id, @RequestBody Vehicle updatedVehicle) {
        updatedVehicle.setId(id);
        return repo.save(updatedVehicle);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicleStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> updates) {
        try {
            Optional<Vehicle> vehicleOpt = repo.findById(id);
            if (!vehicleOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy xe"));
            }

            Vehicle vehicle = vehicleOpt.get();

            // Update battery if provided
            if (updates.containsKey("battery")) {
                Object batteryObj = updates.get("battery");
                if (batteryObj instanceof Number) {
                    vehicle.setBattery(((Number) batteryObj).intValue());
                }
            }

            // Update booking status if provided
            if (updates.containsKey("bookingStatus")) {
                vehicle.setBookingStatus((String) updates.get("bookingStatus"));
            }

            // Update available status if provided
            if (updates.containsKey("available")) {
                vehicle.setAvailable((Boolean) updates.get("available"));
            }

            // Update issue if provided
            if (updates.containsKey("issue")) {
                Object issueObj = updates.get("issue");
                vehicle.setIssue(issueObj != null ? (String) issueObj : null);
            }

            // Update issueSeverity if provided
            if (updates.containsKey("issueSeverity")) {
                Object severityObj = updates.get("issueSeverity");
                vehicle.setIssueSeverity(severityObj != null ? (String) severityObj : null);
            }

            Vehicle updated = repo.save(vehicle);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi cập nhật: " + e.getMessage()));
        }
    }

    @DeleteMapping("/admin/delete/{id}")
    public String deleteVehicle(@PathVariable("id") Long id) {
        repo.deleteById(id);
        return "Delete vehicle " + id + " success";
    }

    @PostMapping("/{id}/report-issue")
    public ResponseEntity<?> reportIssue(@PathVariable("id") Long vehicleId, @RequestBody Map<String, Object> reportData) {
        try {
            Optional<Vehicle> vehicleOpt = repo.findById(vehicleId);
            if (!vehicleOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy xe"));
            }

            Vehicle vehicle = vehicleOpt.get();
            String issue = (String) reportData.get("issue");
            String severity = (String) reportData.get("severity");

            if (issue == null || issue.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mô tả sự cố không được để trống"));
            }

            if (severity == null || severity.trim().isEmpty()) {
                severity = "MODERATE";
            }

            // Get current authenticated user (staff)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String staffUsername = auth.getName();
            Long staffId = null;
            String staffName = staffUsername;
            try {
                Staff staff = staffRepository.findByUsername(staffUsername);
                if (staff != null) {
                    staffId = staff.getId();
                    staffName = staff.getName();
                }
            } catch (Exception e) {
            }

            VehicleReport report = new VehicleReport(
                    vehicleId,
                    vehicle.getPlate(),
                    staffId,
                    staffName,
                    vehicle.getStationId(),
                    issue,
                    severity
            );

            VehicleReport savedReport = vehicleReportRepository.save(report);

            vehicle.setIssue(issue);
            vehicle.setIssueSeverity(severity);
            vehicle.setBookingStatus("MAINTENANCE");
            repo.save(vehicle);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Báo cáo sự cố thành công");
            response.put("report", savedReport);
            response.put("vehicle", vehicle);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi báo cáo sự cố: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/station/{stationId}")
    public ResponseEntity<?> getReportsByStation(@PathVariable("stationId") Long stationId) {
        try {
            List<VehicleReport> reports = vehicleReportRepository.findByStationId(stationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy báo cáo: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/vehicle/{vehicleId}")
    public ResponseEntity<?> getReportsByVehicle(@PathVariable("vehicleId") Long vehicleId) {
        try {
            List<VehicleReport> reports = vehicleReportRepository.findByVehicleId(vehicleId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy báo cáo: " + e.getMessage()));
        }
    }

    @GetMapping("/reports/status/{status}")
    public ResponseEntity<?> getReportsByStatus(@PathVariable("status") String status) {
        try {
            List<VehicleReport> reports = vehicleReportRepository.findByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi lấy báo cáo: " + e.getMessage()));
        }
    }

    private void releaseExpiredHolds(Long stationId) {
        List<RentalRecord> expired = rentalRepo.findByStatusAndHoldExpiresAtBefore(
                "PENDING_PAYMENT", LocalDateTime.now()
        );

        for (RentalRecord record : expired) {
            if (stationId != null && !stationId.equals(record.getStationId())) continue;

            record.setStatus("CANCELLED");
            record.setPaymentStatus("EXPIRED");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.releaseHold(record.getVehicleId(), record.getId());
        }
    }
}
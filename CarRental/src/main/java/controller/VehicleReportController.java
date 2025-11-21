package CarRental.example.controller;

import CarRental.example.document.VehicleReport;
import CarRental.example.repository.VehicleReportRepository;
import CarRental.example.service.VehicleReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicle-reports")
public class VehicleReportController {

    @Autowired
    private VehicleReportService vehicleReportService;

    @Autowired
    private VehicleReportRepository vehicleReportRepository;

    @GetMapping("/all")
    public ResponseEntity<?> getAllReports() {
        try {
            List<VehicleReport> reports = vehicleReportService.getAllReports();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<?> getReportsByStation(@PathVariable("stationId") String stationId) {
        try {
            List<VehicleReport> reports = vehicleReportService.getReportsByStationId(stationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<?> getReportsByVehicle(@PathVariable("vehicleId") String vehicleId) {
        try {
            List<VehicleReport> reports = vehicleReportService.getReportsByVehicleId(vehicleId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getReportsByStatus(@PathVariable("status") String status) {
        try {
            List<VehicleReport> reports = vehicleReportService.getReportsByStatus(status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/station/{stationId}/status/{status}")
    public ResponseEntity<?> getReportsByStationAndStatus(
            @PathVariable("stationId") String stationId,
            @PathVariable("status") String status) {
        try {
            List<VehicleReport> reports = vehicleReportService.getReportsByStationIdAndStatus(stationId, status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", reports.size(),
                    "reports", reports
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReportById(@PathVariable("reportId") String reportId) {
        try {
            Optional<VehicleReport> report = vehicleReportService.getReportById(reportId);
            if (report.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "report", report.get()
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy báo cáo"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable("reportId") String reportId,
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status không được để trống"));
            }

            VehicleReport updated = vehicleReportService.updateReportStatus(reportId, newStatus);
            if (updated != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Cập nhật status thành công",
                        "report", updated
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy báo cáo"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/{reportId}/notes")
    public ResponseEntity<?> addNotesToReport(
            @PathVariable("reportId") String reportId,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            if (notes == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ghi chú không được để trống"));
            }

            VehicleReport updated = vehicleReportService.addNoteToReport(reportId, notes);
            if (updated != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thêm ghi chú thành công",
                        "report", updated
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy báo cáo"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(@PathVariable("reportId") String reportId) {
        try {
            vehicleReportService.deleteReport(reportId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xóa báo cáo thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
}


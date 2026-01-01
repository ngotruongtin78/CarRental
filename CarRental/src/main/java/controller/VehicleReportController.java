package CarRental.example.controller;

import CarRental.example.document.VehicleReport;
import CarRental.example.document.RentalRecord;
import CarRental.example.repository.VehicleReportRepository;
import CarRental.example.repository.RentalRecordRepository;
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

    @Autowired
    private RentalRecordRepository rentalRecordRepository;

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
    public ResponseEntity<?> getReportsByStation(@PathVariable("stationId") Long stationId) {
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
    public ResponseEntity<?> getReportsByVehicle(@PathVariable("vehicleId") Long vehicleId) {
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
            @PathVariable("stationId") Long stationId,
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
    public ResponseEntity<?> getReportById(@PathVariable("reportId") Long reportId) {
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
            @PathVariable("reportId") Long reportId,
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
            @PathVariable("reportId") Long reportId,
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

    /**
     * Lưu ảnh báo cáo vào RentalRecord
     * Nhận binary data từ request body
     */
    @PutMapping("/{reportId}/photo")
    public ResponseEntity<?> saveReportPhoto(
            @PathVariable("reportId") Long reportId,
            @RequestHeader(value = "X-Photo-Name", required = false) String photoName,
            @RequestBody byte[] photoData) {
        try {
            // Tìm báo cáo theo ID
            Optional<VehicleReport> reportOpt = vehicleReportRepository.findById(reportId);
            if (!reportOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy báo cáo"));
            }

            VehicleReport report = reportOpt.get();

            // Tìm RentalRecord dựa trên vehicleId
            // Lấy RentalRecord mới nhất của xe này với status = "WAITING_INSPECTION" hoặc "COMPLETED"
            List<RentalRecord> records = rentalRecordRepository.findAll();
            RentalRecord targetRecord = records.stream()
                    .filter(r -> r.getVehicleId().equals(report.getVehicleId()) &&
                               ("WAITING_INSPECTION".equals(r.getStatus()) || "COMPLETED".equals(r.getStatus())))
                    .max((r1, r2) -> {
                        long t1 = r1.getEndTime() != null ? r1.getEndTime().hashCode() : 0;
                        long t2 = r2.getEndTime() != null ? r2.getEndTime().hashCode() : 0;
                        return Long.compare(t1, t2);
                    })
                    .orElse(null);

            if (targetRecord == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy hợp đồng thuê xe"));
            }

            // Lưu ảnh (binary data) vào RentalRecord
            targetRecord.setDeliveryPhotoData(photoData);
            rentalRecordRepository.save(targetRecord);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lưu ảnh báo cáo thành công",
                    "rentalId", targetRecord.getId(),
                    "photoSize", photoData.length + " bytes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(@PathVariable("reportId") Long reportId) {
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


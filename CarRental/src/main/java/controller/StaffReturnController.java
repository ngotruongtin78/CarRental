package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.document.Staff;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api/staff/return")
@CrossOrigin(origins = "*")
public class StaffReturnController {

    @Autowired
    private RentalRecordRepository rentalRecordRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private StaffRepository staffRepository;

    /**
     * Lấy danh sách các xe sẵn sàng trả từ khách hàng
     * Điều kiện:
     * - RentalRecord có status = "DELIVERED"
     * - RentalRecord.stationId = Staff.stationId (của staff hiện tại)
     */
    @GetMapping("/vehicles-ready")
    public ResponseEntity<?> getVehiclesReadyForReturn() {
        try {
            // Lấy thông tin staff hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa xác thực. Vui lòng đăng nhập lại."));
            }

            String username = null;
            Object principal = authentication.getPrincipal();

            // Handle different types of principal
            if (principal instanceof String) {
                username = (String) principal;
            } else if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal != null) {
                username = principal.toString();
            }

            if (username == null || username.isEmpty() || "anonymousUser".equals(username)) {
                return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy thông tin đăng nhập. Username: " + username));
            }

            Staff staff = staffRepository.findByUsername(username);

            if (staff == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy thông tin nhân viên trong database. Username: " + username));
            }

            String staffStationId = staff.getStationId();

            // Lọc RentalRecord:
            // - status = "WAITING_INSPECTION" (khách đã yêu cầu trả)
            // - stationId của hợp đồng = stationId của staff
            List<RentalRecord> readyRecords = rentalRecordRepository.findAll().stream()
                    .filter(record -> "WAITING_INSPECTION".equalsIgnoreCase(record.getStatus()) &&
                                     staffStationId.equals(record.getStationId()))
                    .toList();

            List<Map<String, Object>> result = new ArrayList<>();

            for (RentalRecord record : readyRecords) {
                Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
                if (vehicle != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", record.getId());
                    item.put("vehiclePlate", vehicle.getPlate());
                    item.put("vehicleType", vehicle.getType());
                    item.put("username", record.getUsername());
                    item.put("startDate", record.getStartDate());
                    item.put("endDate", record.getEndDate());
                    item.put("rentalDays", record.getRentalDays());
                    item.put("total", record.getTotal());
                    item.put("paymentStatus", record.getPaymentStatus());
                    item.put("status", record.getStatus());
                    item.put("returnNotes", record.getReturnNotes());
                    item.put("returnLatitude", record.getReturnLatitude());
                    item.put("returnLongitude", record.getReturnLongitude());
                    item.put("returnTime", record.getEndTime());
                    result.add(item);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }

    /**
     * Xác nhận trả xe từ khách hàng
     * Điều kiện: stationId của hợp đồng phải = stationId của staff
     *
     * Cập nhật RentalRecord:
     * - status = "COMPLETED"
     * - damageFee (từ tham số)
     * - returnNote (ghi chú trả xe)
     * - total được cộng thêm damageFee
     *
     * Cập nhật Vehicle:
     * - available = true (xe trở lại trạm)
     * - bookingStatus = "AVAILABLE" (xe có sẵn để thuê)
     */
    @PostMapping("/{rentalId}/confirm")
    public ResponseEntity<?> confirmReturn(
            @PathVariable("rentalId") String rentalId,
            HttpServletRequest request
    ) {
        try {
            // Lấy damageFee và returnNote từ query parameters
            String damageFeeStr = request.getParameter("damageFee");
            String returnNote = request.getParameter("returnNote");

            double damageFee = 0;
            if (damageFeeStr != null && !damageFeeStr.isEmpty()) {
                damageFee = Double.parseDouble(damageFeeStr);
            }

            // Step 1: Get rental
            Optional<RentalRecord> opt = rentalRecordRepository.findById(rentalId);

            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn thuê"));
            }

            RentalRecord record = opt.get();

            // Step 2: Update status to COMPLETED
            record.setStatus("COMPLETED");

            // Step 3: Add damage fee
            if (damageFee > 0) {
                record.setDamageFee(damageFee);
                record.setTotal(record.getTotal() + damageFee);
                record.setAdditionalFeeAmount(damageFee);
                record.setAdditionalFeePaidAmount(record.getAdditionalFeePaidAmount() != null
                        ? record.getAdditionalFeePaidAmount()
                        : 0.0);
            } else {
                record.setAdditionalFeeAmount(0.0);
                record.setAdditionalFeePaidAmount(record.getAdditionalFeePaidAmount() != null
                        ? record.getAdditionalFeePaidAmount()
                        : 0.0);
            }

            if (returnNote != null && !returnNote.isEmpty()) {
                record.setReturnNotes(returnNote);
                record.setAdditionalFeeNote(returnNote);
            }

            // Step 5: Save
            rentalRecordRepository.save(record);

            // Step 6: Update vehicle status - xe trả về trạm
            Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setAvailable(true);
                vehicle.setBookingStatus("AVAILABLE");
                vehicle.setPendingRentalId(null);
                vehicleRepository.save(vehicle);
            }

            return ResponseEntity.ok(Map.of(
                "message", "Trả xe thành công",
                "rentalId", rentalId,
                "rentalStatus", "COMPLETED",
                "paymentStatus", record.getPaymentStatus(),
                "vehicleStatus", "AVAILABLE"
            ));

        } catch (NumberFormatException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Phí hư hỏng không hợp lệ: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin chi tiết một đơn thuê để trả xe
     * Điều kiện: stationId của hợp đồng phải = stationId của staff
     */
    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getReturnDetails(@PathVariable("rentalId") String rentalId) {
        try {
            // Lấy thông tin staff hiện tại
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Chưa xác thực. Vui lòng đăng nhập lại."));
            }

            String username = null;
            Object principal = authentication.getPrincipal();

            // Handle different types of principal
            if (principal instanceof String) {
                username = (String) principal;
            } else if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal != null) {
                username = principal.toString();
            }

            if (username == null || username.isEmpty() || "anonymousUser".equals(username)) {
                return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy thông tin đăng nhập. Username: " + username));
            }

            Staff staff = staffRepository.findByUsername(username);
            if (staff == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Không tìm thấy thông tin nhân viên trong database. Username: " + username));
            }

            Optional<RentalRecord> optionalRecord = rentalRecordRepository.findById(rentalId);
            if (optionalRecord.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn thuê"));
            }

            RentalRecord record = optionalRecord.get();

            // Kiểm tra stationId: hợp đồng phải cùng trạm với staff
            if (staff.getStationId() == null || !staff.getStationId().equals(record.getStationId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem đơn thuê này (khác trạm)"));
            }

            Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", record.getId());
            result.put("vehiclePlate", vehicle != null ? vehicle.getPlate() : "");
            result.put("vehicleType", vehicle != null ? vehicle.getType() : "");
            result.put("username", record.getUsername());
            result.put("startDate", record.getStartDate());
            result.put("endDate", record.getEndDate());
            result.put("rentalDays", record.getRentalDays());
            result.put("total", record.getTotal());
            result.put("paymentStatus", record.getPaymentStatus());
            result.put("status", record.getStatus());
            result.put("returnNotes", record.getReturnNotes());
            result.put("returnLatitude", record.getReturnLatitude());
            result.put("returnLongitude", record.getReturnLongitude());
            result.put("returnTime", record.getEndTime());
            result.put("checkinLatitude", record.getCheckinLatitude());
            result.put("checkinLongitude", record.getCheckinLongitude());
            result.put("checkinTime", record.getStartTime());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }
}


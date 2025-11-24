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
@RequestMapping("/api/staff/deliver")
@CrossOrigin(origins = "*")
public class StaffDeliverController {

    @Autowired
    private RentalRecordRepository rentalRecordRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private StaffRepository staffRepository;

    /**
     * Lấy danh sách các xe sẵn sàng giao cho khách hàng
     * Điều kiện:
     * - RentalRecord có paymentStatus = "PAID" hoặc "PAY_AT_STATION"
     * - RentalRecord.stationId = Staff.stationId (của staff hiện tại)
     */
    @GetMapping("/vehicles-ready")
    public ResponseEntity<?> getVehiclesReadyForDelivery() {
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
            // - status = "ACTIVE" (chỉ hiển thị những xe sẵn sàng giao)
            // - paymentStatus = "PAID" hoặc "PAY_AT_STATION"
            // - stationId của hợp đồng = stationId của staff
            List<RentalRecord> readyRecords = rentalRecordRepository.findAll().stream()
                    .filter(record -> "ACTIVE".equals(record.getStatus()) &&
                                     ("PAID".equals(record.getPaymentStatus()) ||
                                     "PAY_AT_STATION".equals(record.getPaymentStatus())) &&
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
                    result.add(item);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }

    /**
     * Xác nhận giao xe cho khách hàng
     * Điều kiện: stationId của hợp đồng phải = stationId của staff
     *
     * Cập nhật RentalRecord:
     * - status = "DELIVERED"
     * - paymentStatus = "PAID" (nếu hiện tại là "PAY_AT_STATION")
     * - returnNotes từ ghi chú nhân viên
     *
     * Cập nhật Vehicle:
     * - bookingStatus = "RENTED" (xe đã được giao cho khách)
     * - available = false
     */
    @PostMapping("/{rentalId}/confirm")
    public ResponseEntity<?> confirmDelivery(
            @PathVariable("rentalId") String rentalId,
            HttpServletRequest request
    ) {
        System.out.println("\n=== START confirmDelivery ===");
        System.out.println("rentalId: " + rentalId);

        try {
            // Lấy returnNotes từ query parameter
            String returnNotes = request.getParameter("returnNotes");
            System.out.println("returnNotes: " + returnNotes);

            // Step 1: Get rental
            System.out.println("Step 1: Looking for RentalRecord ID=" + rentalId);
            Optional<RentalRecord> opt = rentalRecordRepository.findById(rentalId);

            if (!opt.isPresent()) {
                System.out.println("ERROR: RentalRecord not found!");
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn thuê"));
            }

            RentalRecord record = opt.get();
            System.out.println("Step 2: RentalRecord found. Current status=" + record.getStatus());

            // Step 2: Update status
            System.out.println("Step 3: Updating status to DELIVERED");
            record.setStatus("DELIVERED");

            // Step 3: Update payment status if needed
            if ("PAY_AT_STATION".equals(record.getPaymentStatus())) {
                System.out.println("Step 4: Updating paymentStatus to PAID");
                record.setPaymentStatus("PAID");
            }

            // Step 4: Add return notes if provided
            if (returnNotes != null && !returnNotes.isEmpty()) {
                System.out.println("Step 5: Adding returnNotes");
                record.setReturnNotes(returnNotes);
            }

            // Step 5: Save
            System.out.println("Step 6: Saving RentalRecord");
            rentalRecordRepository.save(record);
            System.out.println("SUCCESS: RentalRecord saved!");

            // Step 6: Update vehicle status
            System.out.println("Step 7: Updating Vehicle");
            Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setBookingStatus("RENTED");
                vehicle.setAvailable(false);
                vehicle.setPendingRentalId(null);
                vehicleRepository.save(vehicle);
                System.out.println("Vehicle saved successfully");
            } else {
                System.out.println("WARNING: Vehicle not found ID=" + record.getVehicleId());
            }

            System.out.println("=== END confirmDelivery - SUCCESS ===\n");
            return ResponseEntity.ok(Map.of(
                "message", "Giao xe thành công",
                "rentalId", rentalId,
                "rentalStatus", "DELIVERED",
                "paymentStatus", record.getPaymentStatus(),
                "vehicleStatus", "RENTED"
            ));

        } catch (Exception e) {
            System.err.println("\n=== ERROR confirmDelivery ===");
            System.err.println("Exception: " + e.getClass().getSimpleName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===\n");

            return ResponseEntity.status(500).body(Map.of(
                "error", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy thông tin chi tiết một đơn thuê để giao xe
     * Điều kiện: stationId của hợp đồng phải = stationId của staff
     */
    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getDeliveryDetails(@PathVariable("rentalId") String rentalId) {
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
            if (!optionalRecord.isPresent()) {
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

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("ERROR in getDeliveryDetails: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getClass().getSimpleName() + " - " + e.getMessage()));
        }
    }

    /**
     * Lưu ảnh giao xe vào RentalRecord
     * Nhận binary data từ request body
     */
    @PutMapping("/{rentalId}/photo")
    public ResponseEntity<?> saveDeliveryPhoto(
            @PathVariable("rentalId") String rentalId,
            @RequestHeader(value = "X-Photo-Name", required = false) String photoName,
            @RequestBody byte[] photoData) {
        try {
            Optional<RentalRecord> recordOpt = rentalRecordRepository.findById(rentalId);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn thuê"));
            }

            RentalRecord record = recordOpt.get();

            // Lưu ảnh (binary data) vào RentalRecord
            record.setDeliveryPhotoData(photoData);
            rentalRecordRepository.save(record);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lưu ảnh giao xe thành công",
                    "rentalId", rentalId,
                    "photoSize", photoData.length + " bytes"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }
}

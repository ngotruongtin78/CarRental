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

    @GetMapping("/vehicles-ready")
    public ResponseEntity<?> getVehiclesReadyForDelivery() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
            Staff staff = staffRepository.findByUsername(authentication.getName());
            if (staff == null) return ResponseEntity.status(401).build();

            String staffStationId = staff.getStationId();
            List<RentalRecord> readyRecords = rentalRecordRepository.findAll().stream()
                    .filter(record -> "IN_PROGRESS".equals(record.getStatus()) &&
                            ("PAID".equals(record.getPaymentStatus()) || "PAY_AT_STATION".equals(record.getPaymentStatus())) &&
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
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{rentalId}/confirm")
    public ResponseEntity<?> confirmDelivery(@PathVariable("rentalId") String rentalId, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentStaffId = null;
            if (authentication != null) {
                Staff staff = staffRepository.findByUsername(authentication.getName());
                if (staff != null) currentStaffId = staff.getId();
            }

            String returnNotes = request.getParameter("returnNotes");
            Optional<RentalRecord> opt = rentalRecordRepository.findById(rentalId);
            if (!opt.isPresent()) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn thuê"));

            RentalRecord record = opt.get();
            record.setStatus("WAITING_INSPECTION");

            if ("PAY_AT_STATION".equals(record.getPaymentStatus())) record.setPaymentStatus("PAID");
            if (returnNotes != null && !returnNotes.isEmpty()) record.setReturnNotes(returnNotes);

            // [MỚI] Lưu staff ID
            if (currentStaffId != null) record.setDeliveryStaffId(currentStaffId);

            rentalRecordRepository.save(record);

            Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setBookingStatus("RENTED");
                vehicle.setAvailable(false);
                vehicle.setPendingRentalId(null);
                vehicleRepository.save(vehicle);
            }

            // Trả về thông tin chi tiết
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Giao xe thành công");
            response.put("rentalId", rentalId);
            response.put("rentalStatus", record.getStatus());
            response.put("paymentStatus", record.getPaymentStatus());
            response.put("vehicleStatus", vehicle != null ? vehicle.getBookingStatus() : "RENTED");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getDeliveryDetails(@PathVariable("rentalId") String rentalId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
            Staff staff = staffRepository.findByUsername(authentication.getName());
            if (staff == null) return ResponseEntity.status(401).build();

            Optional<RentalRecord> optionalRecord = rentalRecordRepository.findById(rentalId);
            if (!optionalRecord.isPresent()) return ResponseEntity.status(404).build();

            RentalRecord record = optionalRecord.get();
            if (!staff.getStationId().equals(record.getStationId())) return ResponseEntity.status(403).build();

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
            result.put("depositPaidAmount", record.getDepositPaidAmount());
            result.put("depositRequiredAmount", record.getDepositRequiredAmount());
            result.put("status", record.getStatus());
            result.put("returnNotes", record.getReturnNotes());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{rentalId}/photo")
    public ResponseEntity<?> saveDeliveryPhoto(@PathVariable("rentalId") String rentalId, @RequestBody byte[] photoData) {
        Optional<RentalRecord> recordOpt = rentalRecordRepository.findById(rentalId);
        if (!recordOpt.isPresent()) return ResponseEntity.status(404).build();
        RentalRecord record = recordOpt.get();
        record.setDeliveryPhotoData(photoData);
        rentalRecordRepository.save(record);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{rentalId}/signature")
    public ResponseEntity<?> saveDeliverySignature(@PathVariable("rentalId") String rentalId, @RequestBody byte[] signatureData) {
        Optional<RentalRecord> recordOpt = rentalRecordRepository.findById(rentalId);
        if (!recordOpt.isPresent()) return ResponseEntity.status(404).build();
        RentalRecord record = recordOpt.get();
        record.setSignatureData(signatureData);
        rentalRecordRepository.save(record);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
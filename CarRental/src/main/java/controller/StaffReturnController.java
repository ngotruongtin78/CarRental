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

    @GetMapping("/vehicles-ready")
    public ResponseEntity<?> getVehiclesReadyForReturn() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
            Staff staff = staffRepository.findByUsername(authentication.getName());
            if (staff == null) return ResponseEntity.status(401).build();

            String staffStationId = staff.getStationId();
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
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{rentalId}/confirm")
    public ResponseEntity<?> confirmReturn(@PathVariable("rentalId") String rentalId, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentStaffId = null;
            if (authentication != null) {
                Staff staff = staffRepository.findByUsername(authentication.getName());
                if (staff != null) currentStaffId = staff.getId();
            }

            String damageFeeStr = request.getParameter("damageFee");
            String returnNote = request.getParameter("returnNote");
            double damageFee = (damageFeeStr != null && !damageFeeStr.isEmpty()) ? Double.parseDouble(damageFeeStr) : 0;

            Optional<RentalRecord> opt = rentalRecordRepository.findById(rentalId);
            if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Not found"));

            RentalRecord record = opt.get();
            record.setStatus("COMPLETED");

            if (damageFee > 0) {
                record.setDamageFee(damageFee);
                record.setTotal(record.getTotal() + damageFee);
                record.setAdditionalFeeAmount(damageFee);
                record.setAdditionalFeePaidAmount(record.getAdditionalFeePaidAmount() != null ? record.getAdditionalFeePaidAmount() : 0.0);
            }
            if (returnNote != null && !returnNote.isEmpty()) {
                record.setReturnNotes(returnNote);
                record.setAdditionalFeeNote(returnNote);
            }

            // [MỚI] Lưu staff ID
            if (currentStaffId != null) record.setReturnStaffId(currentStaffId);

            rentalRecordRepository.save(record);

            Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
            if (vehicle != null) {
                vehicle.setAvailable(true);
                vehicle.setBookingStatus("AVAILABLE");
                vehicle.setPendingRentalId(null);
                vehicleRepository.save(vehicle);
            }

            return ResponseEntity.ok(Map.of("message", "Trả xe thành công", "rentalId", rentalId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getReturnDetails(@PathVariable("rentalId") String rentalId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) return ResponseEntity.status(401).build();
            Staff staff = staffRepository.findByUsername(authentication.getName());
            if (staff == null) return ResponseEntity.status(401).build();

            Optional<RentalRecord> optionalRecord = rentalRecordRepository.findById(rentalId);
            if (optionalRecord.isEmpty()) return ResponseEntity.status(404).build();

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
            result.put("status", record.getStatus());
            result.put("returnNotes", record.getReturnNotes());
            result.put("returnLatitude", record.getReturnLatitude());
            result.put("returnLongitude", record.getReturnLongitude());
            result.put("returnTime", record.getEndTime());
            result.put("checkinLatitude", record.getCheckinLatitude());
            result.put("checkinLongitude", record.getCheckinLongitude());
            result.put("checkinTime", record.getStartTime());
            result.put("deliveryPhotoData", record.getDeliveryPhotoData());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{rentalId}/photo")
    public ResponseEntity<?> saveReturnPhoto(@PathVariable("rentalId") String rentalId, @RequestBody byte[] photoData) {
        Optional<RentalRecord> recordOpt = rentalRecordRepository.findById(rentalId);
        if (!recordOpt.isPresent()) return ResponseEntity.status(404).build();
        RentalRecord record = recordOpt.get();
        record.setReturnPhotoData(photoData);
        rentalRecordRepository.save(record);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/{rentalId}/receive-photo")
    public ResponseEntity<?> saveReceivePhoto(@PathVariable("rentalId") String rentalId, @RequestBody byte[] photoData) {
        Optional<RentalRecord> recordOpt = rentalRecordRepository.findById(rentalId);
        if (!recordOpt.isPresent()) return ResponseEntity.status(404).build();
        RentalRecord record = recordOpt.get();
        record.setReceivePhotoData(photoData);
        rentalRecordRepository.save(record);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
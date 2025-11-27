package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff/handover")
public class StaffHandoverController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StaffHandoverController(RentalRecordRepository rentalRepo, VehicleRepository vehicleRepo) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listHandovers() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            System.out.println("=== API /api/staff/handover/list called ===");

            List<RentalRecord> allRecords = rentalRepo.findAll();
            System.out.println("Total records found: " + allRecords.size());

            for (RentalRecord record : allRecords) {
                String paymentStatus = Optional.ofNullable(record.getPaymentStatus()).orElse("").trim().toUpperCase();
                String status = Optional.ofNullable(record.getStatus()).orElse("").trim().toUpperCase();

                boolean cancelled = status.equals("CANCELLED") || status.equals("EXPIRED")
                        || paymentStatus.equals("CANCELLED") || paymentStatus.equals("EXPIRED");
                if (cancelled) {
                    System.out.println("Skipping record " + record.getId() + " - cancelled/expired");
                    continue;
                }

                boolean readyForHandover = paymentStatus.equals("PAID") || paymentStatus.equals("DEPOSIT_PENDING")
                        || status.equals("CONTRACT_SIGNED") || status.equals("IN_PROGRESS");

                if (!readyForHandover) {
                    System.out.println("Skipping record " + record.getId() + " - status: " + status + " payment: " + paymentStatus);
                    continue;
                }

                Vehicle vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
                String licensePlate = vehicle != null ? vehicle.getPlate() : "N/A";
                String vehicleType = vehicle != null ? vehicle.getType() : "N/A";

                Map<String, Object> item = new HashMap<>();
                item.put("id", record.getId());
                item.put("licensePlate", licensePlate);
                item.put("vehicleType", vehicleType);
                item.put("renterName", record.getUsername());
                item.put("startDate", record.getStartDate() != null ? record.getStartDate().format(dateFormatter) : null);
                item.put("endDate", record.getEndDate() != null ? record.getEndDate().format(dateFormatter) : null);
                item.put("rentalPrice", record.getTotal());
                item.put("paymentStatus", paymentStatus);
                item.put("paymentMethod", record.getPaymentMethod());
                item.put("notes", record.getCheckinNotes());

                result.add(item);
                System.out.println("Added record: " + record.getId());
            }

            System.out.println("Total items returned: " + result.size());
        } catch (Exception e) {
            System.err.println("Error in listHandovers: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getHandoverDetail(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("=== Getting handover detail for id: " + id + " ===");

            RentalRecord record = rentalRepo.findById(id).orElse(null);
            if (record == null) {
                System.out.println("Record not found for id: " + id);
                response.put("error", "Không tìm thấy hợp đồng thuê");
                return response;
            }

            System.out.println("Record found: " + record.getId());
            System.out.println("VehicleId: " + record.getVehicleId());

            Vehicle vehicle = null;
            if (record.getVehicleId() != null) {
                vehicle = vehicleRepo.findById(record.getVehicleId()).orElse(null);
            }

            String licensePlate = vehicle != null ? vehicle.getPlate() : "N/A";
            String vehicleType = vehicle != null ? vehicle.getType() : "N/A";
            String brand = vehicle != null ? vehicle.getBrand() : "N/A";

            try {
                response.put("id", record.getId());
                response.put("licensePlate", licensePlate);
                response.put("vehicleType", vehicleType);
                response.put("brand", brand);
                response.put("renterName", record.getUsername() != null ? record.getUsername() : "N/A");

                // Format dates safely
                String startDateStr = null;
                if (record.getStartDate() != null) {
                    try {
                        startDateStr = record.getStartDate().format(dateFormatter);
                    } catch (Exception e) {
                        System.err.println("Error formatting startDate: " + e.getMessage());
                        startDateStr = record.getStartDate().toString();
                    }
                }
                response.put("startDate", startDateStr);

                String endDateStr = null;
                if (record.getEndDate() != null) {
                    try {
                        endDateStr = record.getEndDate().format(dateFormatter);
                    } catch (Exception e) {
                        System.err.println("Error formatting endDate: " + e.getMessage());
                        endDateStr = record.getEndDate().toString();
                    }
                }
                response.put("endDate", endDateStr);

                response.put("rentalDays", record.getRentalDays());
                response.put("total", record.getTotal());
                response.put("paymentMethod", record.getPaymentMethod() != null ? record.getPaymentMethod() : "N/A");
                response.put("paymentStatus", record.getPaymentStatus() != null ? record.getPaymentStatus() : "N/A");
                response.put("status", record.getStatus() != null ? record.getStatus() : "N/A");
                response.put("checkinNotes", record.getCheckinNotes() != null ? record.getCheckinNotes() : "");
                response.put("checkinLatitude", record.getCheckinLatitude());
                response.put("checkinLongitude", record.getCheckinLongitude());
                response.put("checkinTime", record.getStartTime());
                response.put("returnNotes", record.getReturnNotes());
                response.put("returnLatitude", record.getReturnLatitude());
                response.put("returnLongitude", record.getReturnLongitude());
                response.put("returnTime", record.getEndTime());

                System.out.println("Handover detail prepared successfully");
            } catch (Exception innerE) {
                System.err.println("Error preparing response data: " + innerE.getMessage());
                innerE.printStackTrace();
                response.put("error", "Lỗi khi xử lý dữ liệu: " + innerE.getMessage());
                return response;
            }

            return response;
        } catch (Exception e) {
            System.err.println("Error in getHandoverDetail: " + e.getMessage());
            e.printStackTrace();
            response.put("error", "Lỗi: " + e.getMessage());
            return response;
        }
    }

    @PostMapping("/confirm/{id}")
    public Map<String, Object> confirmHandover(@PathVariable("id") String id) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("Confirming handover for id: " + id);

            RentalRecord record = rentalRepo.findById(id).orElse(null);
            if (record == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy hợp đồng thuê");
                return response;
            }

            // Cập nhật trạng thái thành "ACTIVE"
            record.setStatus("ACTIVE");
            rentalRepo.save(record);

            response.put("success", true);
            response.put("message", "Xác nhận giao xe thành công");
            System.out.println("Handover confirmed successfully for id: " + id);
            return response;
        } catch (Exception e) {
            System.err.println("Error in confirmHandover: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return response;
        }
    }
}


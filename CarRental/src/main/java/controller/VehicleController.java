package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.VehicleService;

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
    @GetMapping("/station/{stationId}")
    public List<Vehicle> getByStation(@PathVariable("stationId") String stationId) {
        releaseExpiredHolds(stationId);
        return repo.findByStationIdAndBookingStatusNot(stationId, "RENTED");
    }
    @GetMapping("/admin/all")
    public List<Vehicle> getAllVehicles() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Vehicle> getVehicle(@PathVariable("id") String id) {
        return repo.findById(id);
    }

    @GetMapping("/admin/{id}")
    public Optional<Vehicle> getVehicleById(@PathVariable("id") String id) {
        return repo.findById(id);
    }

    @PostMapping("/admin/add")
    public Vehicle addVehicle(@RequestBody Vehicle vehicle) {
        return repo.save(vehicle);
    }

    @PutMapping("/admin/update/{id}")
    public Vehicle updateVehicle(@PathVariable("id") String id, @RequestBody Vehicle updatedVehicle) {
        updatedVehicle.setId(id);
        return repo.save(updatedVehicle);
    }

    @DeleteMapping("/admin/delete/{id}")
    public String deleteVehicle(@PathVariable("id") String id) {
        repo.deleteById(id);
        return "Delete vehicle " + id + " success";
    }

    private void releaseExpiredHolds(String stationId) {
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
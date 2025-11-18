package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/return")
public class StaffReturnController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;

    public StaffReturnController(RentalRecordRepository rentalRepo, VehicleRepository vehicleRepo) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
    }

    @PostMapping("/{id}/confirm")
    public String confirmReturn(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") double damageFee
    ) {
        RentalRecord rental = rentalRepo.findById(id).orElse(null);
        if (rental == null) return "RENTAL_NOT_FOUND";

        rental.setDamageFee(damageFee);
        rental.setTotal(rental.getTotal() + damageFee);
        rental.setStatus("COMPLETED");

        Vehicle vehicle = vehicleRepo.findById(rental.getVehicleId()).orElse(null);
        if (vehicle != null) {
            vehicle.setAvailable(true);
            vehicle.setBookingStatus("AVAILABLE");
            vehicle.setPendingRentalId(null);
            vehicleRepo.save(vehicle);
        }

        rentalRepo.save(rental);
        return "RETURN_CONFIRMED";
    }
}

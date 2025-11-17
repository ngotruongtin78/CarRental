package CarRental.example.service;

import CarRental.example.document.Vehicle;
import CarRental.example.repository.VehicleRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepo;

    public VehicleService(VehicleRepository vehicleRepo) {
        this.vehicleRepo = vehicleRepo;
    }

    public void updateAvailable(String id, boolean available) {
        Vehicle v = vehicleRepo.findById(id).orElse(null);
        if (v != null) {
            v.setAvailable(available);
            vehicleRepo.save(v);
        }
    }
}

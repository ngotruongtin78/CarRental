package CarRental.example.config;

import CarRental.example.document.Vehicle;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StationDataLoader implements CommandLineRunner {

    private final StationRepository stationRepo;
    private final VehicleRepository vehicleRepo;

    public StationDataLoader(StationRepository stationRepo, VehicleRepository vehicleRepo) {
        this.stationRepo = stationRepo;
        this.vehicleRepo = vehicleRepo;
    }

    @Override
    public void run(String... args) {

    }
}
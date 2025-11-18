package CarRental.example.controller;

import CarRental.example.document.Vehicle;
import CarRental.example.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleRepository repo;
    @GetMapping("/station/{stationId}")
    public List<Vehicle> getByStation(@PathVariable("stationId") String stationId) {
        return repo.findByStationIdAndAvailable(stationId, true);
    }
    @GetMapping("/admin/all")
    public List<Vehicle> getAllVehicles() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Vehicle> getVehicle(@PathVariable String id) {
        return repo.findById(id);
    }

    @GetMapping("/admin/{id}")
    public Optional<Vehicle> getVehicleById(@PathVariable String id) {
        return repo.findById(id);
    }

    @PostMapping("/admin/add")
    public Vehicle addVehicle(@RequestBody Vehicle vehicle) {
        return repo.save(vehicle);
    }

    @PutMapping("/admin/update/{id}")
    public Vehicle updateVehicle(@PathVariable String id, @RequestBody Vehicle updatedVehicle) {
        updatedVehicle.setId(id);
        return repo.save(updatedVehicle);
    }

    @DeleteMapping("/admin/delete/{id}")
    public String deleteVehicle(@PathVariable String id) {
        repo.deleteById(id);
        return "Delete vehicle " + id + " success";
    }
}
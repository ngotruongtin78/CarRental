package CarRental.example.controller;

import CarRental.example.document.Station;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.SequenceGeneratorService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final StationRepository stationRepo;
    private final VehicleRepository vehicleRepo;
    private final SequenceGeneratorService sequenceGenerator;
    public StationController(StationRepository stationRepo,
                             VehicleRepository vehicleRepo,
                             SequenceGeneratorService sequenceGenerator) {
        this.stationRepo = stationRepo;
        this.vehicleRepo = vehicleRepo;
        this.sequenceGenerator = sequenceGenerator;
    }

    @GetMapping
    public List<Map<String, Object>> getStations() {

        List<Station> stations = stationRepo.findAll();
        List<Map<String, Object>> data = new ArrayList<>();

        for (Station st : stations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", st.getId());
            map.put("name", st.getName());
            map.put("latitude", st.getLatitude());
            map.put("longitude", st.getLongitude());
            map.put("address", st.getAddress());

            // Dòng quan trọng: Đếm số xe có sẵn và gửi cho frontend
            long count = vehicleRepo.countByStationIdAndAvailable(st.getId(), true);
            map.put("availableCars", count);

            data.add(map);
        }
        return data;
    }

    @GetMapping("/admin/all")
    public List<Station> getStationsForAdmin() {
        return stationRepo.findAll();
    }

    @GetMapping("/admin/{id}")
    public Optional<Station> getStationById(@PathVariable String id) {
        return stationRepo.findById(id);
    }

    @PostMapping("/admin/add")
    public Station addStation(@RequestBody Station station) {
        long seq = sequenceGenerator.getNextSequence("stationCounter");
        String newId = "st" + seq;
        station.setId(newId);
        return stationRepo.save(station);
    }

    @PutMapping("/admin/update/{id}")
    public Station updateStation(@PathVariable String id, @RequestBody Station updatedStation) {
        updatedStation.setId(id);
        return stationRepo.save(updatedStation);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<String> deleteStation(@PathVariable String id) {

        if (vehicleRepo.countByStationIdAndAvailable(id, true) > 0 || vehicleRepo.countByStationIdAndAvailable(id, false) > 0) {
            return new ResponseEntity<>("Không thể xóa trạm vì vẫn còn xe.", HttpStatus.BAD_REQUEST);
        }

        stationRepo.deleteById(id);
        return new ResponseEntity<>("Xóa trạm " + id + " thành công!", HttpStatus.OK);
    }
}
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

    // API cho trang chủ (Khách hàng)
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

            // Cập nhật luôn cho khách hàng để họ thấy đúng số lượng xe sẵn sàng
            long count = vehicleRepo.countAvailableVehiclesRobust(st.getId());
            map.put("availableCars", count);

            data.add(map);
        }
        return data;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStation(@PathVariable("id") String id) {
        Optional<Station> station = stationRepo.findById(id);
        return station.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Station not found"));
    }

    // API cho Admin Dashboard
    @GetMapping("/admin/all")
    public ResponseEntity<List<Map<String, Object>>> getStationsForAdmin() {
        List<Station> stations = stationRepo.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Station st : stations) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", st.getId());
            map.put("name", st.getName());
            map.put("address", st.getAddress());
            map.put("latitude", st.getLatitude());
            map.put("longitude", st.getLongitude());

            // 1. Sẵn sàng: Dùng hàm mới để đếm cả xe cũ lẫn xe mới
            long countAvailable = vehicleRepo.countAvailableVehiclesRobust(st.getId());

            // 2. Đang thuê: Cộng dồn RENTED và PENDING_PAYMENT
            long countRented = vehicleRepo.countByStationIdAndBookingStatus(st.getId(), "RENTED");
            long countPending = vehicleRepo.countByStationIdAndBookingStatus(st.getId(), "PENDING_PAYMENT");

            // 3. Bảo trì
            long countMaintenance = vehicleRepo.countByStationIdAndBookingStatus(st.getId(), "MAINTENANCE");

            map.put("statsAvailable", countAvailable);
            map.put("statsRented", countRented + countPending);
            map.put("statsMaintenance", countMaintenance);

            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{id}")
    public Optional<Station> getStationById(@PathVariable("id") String id) {
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
    public Station updateStation(@PathVariable("id") String id, @RequestBody Station updatedStation) {
        updatedStation.setId(id);
        return stationRepo.save(updatedStation);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<String> deleteStation(@PathVariable("id") String id) {
        // Kiểm tra kỹ trước khi xóa: phải dùng hàm Robust để không xóa nhầm trạm còn xe cũ
        if (vehicleRepo.countAvailableVehiclesRobust(id) > 0 || vehicleRepo.countByStationIdAndAvailable(id, false) > 0) {
            return new ResponseEntity<>("Không thể xóa trạm vì vẫn còn xe.", HttpStatus.BAD_REQUEST);
        }
        stationRepo.deleteById(id);
        return new ResponseEntity<>("Xóa trạm " + id + " thành công!", HttpStatus.OK);
    }
}
package CarRental.example.controller;

import CarRental.example.document.Station;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.VehicleRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final StationRepository stationRepo;
    private final VehicleRepository vehicleRepo;

    public StationController(StationRepository stationRepo,
                             VehicleRepository vehicleRepo) {
        this.stationRepo = stationRepo;
        this.vehicleRepo = vehicleRepo;
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
            long count = vehicleRepo.countAvailableVehiclesRobust(String.valueOf(st.getId()));
            map.put("availableCars", count);

            data.add(map);
        }
        return data;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStation(@PathVariable("id") Long id) {
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
            long countAvailable = vehicleRepo.countAvailableVehiclesRobust(String.valueOf(st.getId()));

            // 2. Đang thuê: Cộng dồn RENTED và PENDING_PAYMENT
            long countRented = vehicleRepo.countByStationIdAndBookingStatus(String.valueOf(st.getId()), "RENTED");
            long countPending = vehicleRepo.countByStationIdAndBookingStatus(String.valueOf(st.getId()), "PENDING_PAYMENT");

            // 3. Bảo trì
            long countMaintenance = vehicleRepo.countByStationIdAndBookingStatus(String.valueOf(st.getId()), "MAINTENANCE");

            map.put("statsAvailable", countAvailable);
            map.put("statsRented", countRented + countPending);
            map.put("statsMaintenance", countMaintenance);

            response.add(map);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{id}")
    public Optional<Station> getStationById(@PathVariable("id") Long id) {
        return stationRepo.findById(id);
    }

    @PostMapping("/admin/add")
    public Station addStation(@RequestBody Station station) {
        // JPA will auto-generate the ID
        return stationRepo.save(station);
    }

    @PutMapping("/admin/update/{id}")
    public Station updateStation(@PathVariable("id") Long id, @RequestBody Station updatedStation) {
        updatedStation.setId(id);
        return stationRepo.save(updatedStation);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<String> deleteStation(@PathVariable("id") Long id) {
        // Kiểm tra kỹ trước khi xóa: phải dùng hàm Robust để không xóa nhầm trạm còn xe cũ
        if (vehicleRepo.countAvailableVehiclesRobust(String.valueOf(id)) > 0 || vehicleRepo.countByStationIdAndAvailable(String.valueOf(id), false) > 0) {
            return new ResponseEntity<>("Không thể xóa trạm vì vẫn còn xe.", HttpStatus.BAD_REQUEST);
        }
        stationRepo.deleteById(id);
        return new ResponseEntity<>("Xóa trạm " + id + " thành công!", HttpStatus.OK);
    }
}
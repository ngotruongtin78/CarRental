package CarRental.example.controller;

import CarRental.example.document.Staff;
import CarRental.example.document.Station;
import CarRental.example.repository.StaffRepository;
import CarRental.example.repository.StationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "*")
class StaffVehicleController {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StationRepository stationRepository;

    @GetMapping("/current-station")
    public ResponseEntity<?> getCurrentStationOfStaff() {
        try {
            // Lấy thông tin user hiện tại từ Authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Tìm staff theo username từ StaffRepository
            Staff staff = staffRepository.findByUsername(username);
            if (staff == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy nhân viên"));
            }

            // Lấy thông tin trạm
            String stationId = staff.getStationId();
            Station station = stationRepository.findById(stationId).orElse(null);

            if (station == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy trạm"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", station.getId());
            response.put("name", station.getName());
            response.put("address", station.getAddress());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi server: " + e.getMessage()));
        }
    }
}
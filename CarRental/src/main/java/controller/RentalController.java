package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.SequenceGeneratorService;
import CarRental.example.service.VehicleService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rental")
public class RentalController {

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepo;
    private final SequenceGeneratorService sequence;
    private final VehicleService vehicleService;
    public RentalController(RentalRecordRepository rentalRepo,
                            VehicleRepository vehicleRepo,
                            SequenceGeneratorService sequence, VehicleService vehicleService) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepo = vehicleRepo;
        this.sequence = sequence;
        this.vehicleService = vehicleService;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> req) {

        String vehicleId = (String) req.get("vehicleId");
        String stationId = (String) req.get("stationId");

        Vehicle v = vehicleRepo.findById(vehicleId).orElse(null);
        if (v == null) {
            return Map.of("error", "Vehicle not found");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("vehicleId", v.getId());
        data.put("vehicleName", v.getBrand());
        data.put("price", v.getPrice());
        data.put("stationId", stationId);

        return data;
    }

    @PostMapping("/create")
    public Map<String, Object> createRental(@RequestBody Map<String, Object> req) {

        String username = getCurrentUsername();
        if (username == null) return Map.of("error", "Unauthorized");

        String vehicleId = (String) req.get("vehicleId");
        String stationId = (String) req.get("stationId");

        double amount;
        Object rawAmount = req.get("amount");
        if (rawAmount instanceof Integer) {
            amount = ((Integer) rawAmount).doubleValue();
        } else {
            amount = (double) rawAmount;
        }

        long seq = sequence.getNextSequence("rentalCounter");
        String rentalId = "rental" + seq;

        RentalRecord record = new RentalRecord();
        record.setId(rentalId);
        record.setUsername(username);
        record.setVehicleId(vehicleId);
        record.setStationId(stationId);
        record.setStartTime(LocalDateTime.now());
        record.setTotal(amount);

        rentalRepo.save(record);

        vehicleRepo.updateAvailable(vehicleId, false);

        return Map.of(
                "status", "success",
                "rentalId", rentalId
        );
    }

    @GetMapping("/history")
    public List<RentalRecord> history() {
        String username = getCurrentUsername();
        return rentalRepo.findByUsername(username);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        String username = getCurrentUsername();
        if (username == null) return Map.of("error", "Unauthorized");

        List<RentalRecord> records = rentalRepo.findByUsername(username);

        double totalSpent = records.stream()
                .mapToDouble(RentalRecord::getTotal)
                .sum();

        int totalTrips = records.size();
        double averageSpent = totalTrips > 0 ? totalSpent / totalTrips : 0;

        Map<Integer, Long> hourCounts = records.stream()
                .filter(r -> r.getStartTime() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getStartTime().getHour(),
                        Collectors.counting()
                ));

        List<Integer> peakHours = hourCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTrips", totalTrips);
        stats.put("totalSpent", totalSpent);
        stats.put("averageSpent", averageSpent);
        stats.put("peakHours", peakHours);

        return stats;
    }
}

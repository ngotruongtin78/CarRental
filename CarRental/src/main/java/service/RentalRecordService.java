package CarRental.example.service;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RentalRecordService {

    private final RentalRecordRepository repo;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;

    public RentalRecordService(RentalRecordRepository repo,
                              VehicleRepository vehicleRepository,
                              StationRepository stationRepository) {
        this.repo = repo;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
    }

    public RentalRecord saveRecord(RentalRecord record) {
        return repo.save(record);
    }

    public List<RentalRecord> getHistoryByUsername(String username) {
        return repo.findByUsername(username);
    }

    public List<Map<String, Object>> getHistoryDetails(String username) {
        List<RentalRecord> records = repo.findByUsername(username);
        List<Map<String, Object>> response = new ArrayList<>();

        for (RentalRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("record", record);

            vehicleRepository.findById(record.getVehicleId()).ifPresent(vehicle -> {
                Map<String, Object> vehicleInfo = new LinkedHashMap<>();
                vehicleInfo.put("id", vehicle.getId());
                vehicleInfo.put("type", vehicle.getType());
                vehicleInfo.put("plate", vehicle.getPlate());
                vehicleInfo.put("brand", vehicle.getBrand());
                vehicleInfo.put("price", vehicle.getPrice());
                item.put("vehicle", vehicleInfo);
            });

            stationRepository.findById(record.getStationId()).ifPresent(station -> {
                Map<String, Object> stationInfo = new LinkedHashMap<>();
                stationInfo.put("id", station.getId());
                stationInfo.put("name", station.getName());
                stationInfo.put("address", station.getAddress());
                item.put("station", stationInfo);
            });

            response.add(item);
        }

        return response;
    }

    public List<RentalRecord> getAll() {
        return repo.findAll();
    }

    public RentalRecord getById(String id) {
        return repo.findById(id).orElse(null);
    }

    public void delete(String id) {
        repo.deleteById(id);
    }

    public Map<String, Object> calculateStats(String username) {
        List<RentalRecord> records = repo.findByUsername(username);

        double totalSpent = records.stream()
                .mapToDouble(RentalRecord::getTotal)
                .sum();

        int totalTrips = records.size();
        double averageSpent = totalTrips > 0 ? totalSpent / totalTrips : 0;

        Map<Integer, Long> hourCounts = new HashMap<>();
        long totalMinutes = 0;
        int countedDurations = 0;

        for (RentalRecord record : records) {
            if (record.getStartTime() != null) {
                hourCounts.merge(record.getStartTime().getHour(), 1L, Long::sum);
            }

            if (record.getStartTime() != null && record.getEndTime() != null) {
                totalMinutes += Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
                countedDurations++;
            }
        }

        List<Integer> peakHours = hourCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        double averageDurationMinutes = countedDurations > 0 ? (double) totalMinutes / countedDurations : 0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTrips", totalTrips);
        stats.put("totalSpent", totalSpent);
        stats.put("averageSpent", averageSpent);
        stats.put("peakHours", peakHours);
        stats.put("averageDurationMinutes", averageDurationMinutes);

        return stats;
    }

    public RentalRecord signContract(String rentalId, String username) {
        RentalRecord record = repo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) return null;

        record.setContractSigned(true);
        record.setStatus("CONTRACT_SIGNED");
        return repo.save(record);
    }

    public RentalRecord checkIn(String rentalId, String username, String notes) {
        RentalRecord record = repo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) return null;

        if (record.getStartTime() == null) {
            record.setStartTime(LocalDateTime.now());
        }

        record.setCheckinNotes(notes);
        record.setStatus("IN_PROGRESS");
        return repo.save(record);
    }

    public RentalRecord requestReturn(String rentalId, String username, String notes) {
        RentalRecord record = repo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) return null;

        record.setReturnNotes(notes);
        record.setEndTime(LocalDateTime.now());
        record.setStatus("WAITING_INSPECTION");
        return repo.save(record);
    }
}

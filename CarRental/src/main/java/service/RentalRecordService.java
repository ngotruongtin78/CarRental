package CarRental.example.service;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.StationRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RentalRecordService {

    private final RentalRecordRepository repo;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;

    public RentalRecordService(RentalRecordRepository repo, VehicleRepository vehicleRepository, StationRepository stationRepository) {
        this.repo = repo;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
    }

    public RentalRecord saveRecord(RentalRecord record) {
        ensureCreatedAt(record);
        return repo.save(record);
    }
    public List<RentalRecord> getHistoryByUsername(String username) { return repo.findByUsername(username); }
    public List<RentalRecord> getAll() { return repo.findAll(); }
    public RentalRecord getById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }

    public List<Map<String, Object>> getHistoryDetails(String username) {
        List<RentalRecord> records = repo.findByUsername(username)
                .stream()
                .filter(this::isVisibleInHistory)
                .sorted(buildHistoryComparator())
                .toList();
        List<Map<String, Object>> response = new ArrayList<>();
        for (RentalRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            StatusView statusView = resolveStatus(record);
            item.put("record", record);
            item.put("displayStatus", statusView.display);
            item.put("filterStatus", statusView.filterKey);
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
                stationInfo.put("latitude", station.getLatitude());
                stationInfo.put("longitude", station.getLongitude());
                item.put("station", stationInfo);
            });
            response.add(item);
        }
        return response;
    }

    public List<Map<String, Object>> getAllHistoryDetails() {
        List<RentalRecord> records = repo.findAll()
                .stream()
                .filter(this::isVisibleInHistory)
                .sorted(buildHistoryComparator())
                .toList();
        List<Map<String, Object>> response = new ArrayList<>();
        for (RentalRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            StatusView statusView = resolveStatus(record);
            item.put("record", record);
            item.put("displayStatus", statusView.display);
            item.put("filterStatus", statusView.filterKey);
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
                stationInfo.put("latitude", station.getLatitude());
                stationInfo.put("longitude", station.getLongitude());
                item.put("station", stationInfo);
            });
            response.add(item);
        }
        return response;
    }

    private long getSortTimestamp(RentalRecord record) {
        if (record == null) return 0;

        List<LocalDateTime> timestamps = new ArrayList<>();

        Optional.ofNullable(record.getEndTime()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getStartTime()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getPaidAt()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getDepositPaidAt()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getAdditionalFeePaidAt()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getHoldExpiresAt()).ifPresent(timestamps::add);
        Optional.ofNullable(record.getCreatedAt()).ifPresent(timestamps::add);
        Optional.ofNullable(toLocalDateTime(record.getStartDate(), false)).ifPresent(timestamps::add);

        // Ưu tiên thời điểm mới nhất giữa các mốc thanh toán, nhận xe và thời điểm tạo đơn.
        LocalDateTime newest = timestamps.stream()
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (newest != null) {
            return newest.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        // Fallback: thời điểm tạo bản ghi dựa trên ObjectId hoặc chuỗi số (ưu tiên hiển thị đơn mới nhất).
        String id = record.getId();
        if (id != null) {
            try {
                return new org.bson.types.ObjectId(id).getTimestamp() * 1000L;
            } catch (IllegalArgumentException ignored) {
                // ID không phải ObjectId, tiếp tục xuống dưới.
            }

            String digits = id.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Long.parseLong(digits);
                } catch (NumberFormatException ignored) {
                    // Fallback to 0 below.
                }
            }
        }

        return 0;
    }

    private Long getCreatedMillis(RentalRecord record) {
        if (record == null) return null;
        LocalDateTime created = record.getCreatedAt();
        if (created == null) return null;
        return created.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Long getObjectIdTimestamp(RentalRecord record) {
        if (record == null || record.getId() == null) return null;
        try {
            return new org.bson.types.ObjectId(record.getId()).getTimestamp() * 1000L;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Long getNumericIdTimestamp(RentalRecord record) {
        if (record == null || record.getId() == null) return null;
        try {
            String digits = record.getId().replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;
            return Long.parseLong(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Comparator<RentalRecord> buildHistoryComparator() {
        return Comparator
                .comparingLong(this::getSortTimestamp).reversed()
                .thenComparing((RentalRecord r) -> getCreatedMillis(r), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((RentalRecord r) -> getObjectIdTimestamp(r), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing((RentalRecord r) -> getNumericIdTimestamp(r), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RentalRecord::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private LocalDateTime toLocalDateTime(LocalDate date, boolean endOfDay) {
        if (date == null) return null;
        return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
    }

    private void ensureCreatedAt(RentalRecord record) {
        if (record != null && record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
    }

    public Map<String, Object> calculateStats(String username) {
        List<RentalRecord> records = repo.findByUsername(username).stream().filter(this::isVisibleInHistory).toList();
        double totalSpent = records.stream().mapToDouble(RentalRecord::getTotal).sum();
        int totalTrips = records.size();
        double averageSpent = totalTrips > 0 ? totalSpent / totalTrips : 0;
        long totalMinutes = 0;
        int countedDurations = 0;
        Map<Integer, Long> hourCounts = new HashMap<>();

        for (RentalRecord record : records) {
            if (record.getStartTime() != null) hourCounts.merge(record.getStartTime().getHour(), 1L, Long::sum);
            if (record.getStartTime() != null && record.getEndTime() != null) {
                totalMinutes += Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
                countedDurations++;
            }
        }
        List<Integer> peakHours = hourCounts.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(3).map(Map.Entry::getKey).toList();
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
        ensureCreatedAt(record);
        return repo.save(record);
    }

    public RentalRecord checkIn(String rentalId, String username, String notes) {
        RentalRecord record = repo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) return null;
        if (record.getStartTime() == null) record.setStartTime(LocalDateTime.now());
        record.setCheckinNotes(notes);
        record.setStatus("IN_PROGRESS");
        ensureCreatedAt(record);
        return repo.save(record);
    }

    public RentalRecord requestReturn(String rentalId, String username, String notes) {
        RentalRecord record = repo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) return null;
        record.setReturnNotes(notes);
        record.setEndTime(LocalDateTime.now());
        record.setStatus("WAITING_INSPECTION");
        ensureCreatedAt(record);
        return repo.save(record);
    }

    public Map<String, Object> getGlobalStats() {
        List<RentalRecord> allRecords = repo.findAll();
        double totalRevenue = 0;
        int totalTrips = 0;
        int successfulTrips = 0;
        Map<String, Double> revenueByDate = new TreeMap<>();
        Map<Integer, Integer> peakHourCounts = new HashMap<>();
        for (int i = 0; i < 24; i++) peakHourCounts.put(i, 0);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (RentalRecord r : allRecords) {
            String status = r.getStatus() != null ? r.getStatus().toUpperCase() : "";
            String paymentStatus = r.getPaymentStatus() != null ? r.getPaymentStatus().toUpperCase() : "";
            boolean isCancelled = status.contains("CANCELLED") || status.contains("EXPIRED") || paymentStatus.contains("CANCELLED") || paymentStatus.contains("EXPIRED");

            if (!isCancelled) {
                totalTrips++;
                double amount = r.getTotal() > 0 ? r.getTotal() : 0;
                totalRevenue += amount;
                successfulTrips++;

                String dateKey = null;
                if (r.getStartDate() != null) dateKey = r.getStartDate().format(dateFormatter);
                else if (r.getStartTime() != null) dateKey = r.getStartTime().format(dateFormatter);
                else dateKey = java.time.LocalDate.now().format(dateFormatter);

                revenueByDate.merge(dateKey, amount, Double::sum);

                int h = 8;
                if (r.getStartTime() != null) h = r.getStartTime().getHour();
                peakHourCounts.merge(h, 1, Integer::sum);
            }
        }

        List<String> revLabels = new ArrayList<>(revenueByDate.keySet());
        List<Double> revValues = new ArrayList<>(revenueByDate.values());
        List<String> peakLabels = new ArrayList<>();
        List<Double> peakValues = new ArrayList<>();

        for (int h = 0; h <= 23; h++) {
            peakLabels.add(h + "h");
            int count = peakHourCounts.getOrDefault(h, 0);
            double percent = totalTrips > 0 ? ((double) count / totalTrips) * 100 : 0;
            peakValues.add(Math.round(percent * 10.0) / 10.0);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalTrips", totalTrips);
        stats.put("avgUtilization", totalTrips > 0 ? (double) successfulTrips / totalTrips : 0);
        stats.put("revenueChartLabels", revLabels);
        stats.put("revenueChartValues", revValues);
        stats.put("peakLabels", peakLabels);
        stats.put("peakValues", peakValues);
        return stats;
    }

    public List<String> getAiSuggestions() {
        List<String> suggestions = new ArrayList<>();
        List<RentalRecord> allRecords = repo.findAll();
        Map<String, Long> tripsByStation = allRecords.stream()
                .filter(r -> !"CANCELLED".equals(r.getStatus()))
                .collect(Collectors.groupingBy(RentalRecord::getStationId, Collectors.counting()));

        tripsByStation.forEach((stationId, count) -> {
            String stationName = stationRepository.findById(stationId).map(s -> s.getName()).orElse(stationId);
            long currentVehicles = vehicleRepository.findByStationIdAndBookingStatusNot(stationId, "MAINTENANCE").size();
            if (currentVehicles > 0 && (count / currentVehicles) >= 5) {
                suggestions.add("🔥 <strong>Nhu cầu cao tại " + stationName + ":</strong> AI khuyến nghị bổ sung thêm xe.");
            } else if (currentVehicles > 5 && count < currentVehicles) {
                suggestions.add("⚠️ <strong>Dư thừa tại " + stationName + ":</strong> Cần điều chuyển bớt xe.");
            }
        });
        if (suggestions.isEmpty()) suggestions.add("ℹ️ <strong>Hệ thống:</strong> Dữ liệu ổn định.");
        return suggestions;
    }

    private boolean isVisibleInHistory(RentalRecord record) {
        if (record == null) return false;
        String status = Optional.ofNullable(record.getStatus()).orElse("").toUpperCase();
        String paymentStatus = Optional.ofNullable(record.getPaymentStatus()).orElse("").toUpperCase();
        return !status.equals("CANCELLED") && !status.equals("EXPIRED") && !paymentStatus.equals("CANCELLED") && !paymentStatus.equals("EXPIRED");
    }

    private StatusView resolveStatus(RentalRecord record) {
        String status = Optional.ofNullable(record.getStatus()).orElse("").toUpperCase();
        String paymentStatus = Optional.ofNullable(record.getPaymentStatus()).orElse("").toUpperCase();
        String paymentMethod = Optional.ofNullable(record.getPaymentMethod()).orElse("").toLowerCase();

        if (status.equals("RETURNED") || status.equals("COMPLETED")) return new StatusView("Đã trả xe", "returned");
        if (status.equals("WAITING_INSPECTION")) return new StatusView("Chờ xác nhận trả", "returned");
        if (paymentStatus.equals("PAID") || status.equals("PAID") || status.equals("IN_PROGRESS") || status.equals("CONTRACT_SIGNED")) return new StatusView("Đang thuê", "active");
        if (paymentMethod.equals("cash") || paymentStatus.equals("PAY_AT_STATION") || status.equals("PENDING_PAYMENT")) return new StatusView("Đang chờ thanh toán", "rented");

        return new StatusView("Đã thuê", "rented");
    }
    private record StatusView(String display, String filterKey) {}
}
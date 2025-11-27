package CarRental.example.controller;

import CarRental.example.document.RentalRating;
import CarRental.example.document.RentalRecord;
import CarRental.example.document.User;
import CarRental.example.repository.RentalRatingRepository;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.UserRepository;
import CarRental.example.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RentalRatingRepository rentalRatingRepository;
    private final RentalRecordRepository rentalRecordRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public RatingController(RentalRatingRepository rentalRatingRepository,
                            RentalRecordRepository rentalRecordRepository,
                            UserRepository userRepository,
                            VehicleRepository vehicleRepository) {
        this.rentalRatingRepository = rentalRatingRepository;
        this.rentalRecordRepository = rentalRecordRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName());
    }

    @PostMapping
    public ResponseEntity<?> submitRating(@RequestBody Map<String, Object> payload) {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        String rentalId = Objects.toString(payload.get("rentalId"), "");
        if (rentalId.isEmpty()) return ResponseEntity.badRequest().body("Thiếu thông tin chuyến thuê.");

        Optional<RentalRecord> rentalOpt = rentalRecordRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) return ResponseEntity.status(404).body("Không tìm thấy chuyến thuê");
        RentalRecord record = rentalOpt.get();
        if (!Objects.equals(record.getUserId(), user.getId()) && !Objects.equals(record.getUsername(), user.getUsername())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        String status = Optional.ofNullable(record.getStatus()).orElse("").toUpperCase();
        if (!Arrays.asList("RETURNED", "COMPLETED", "WAITING_INSPECTION").contains(status)) {
            return ResponseEntity.badRequest().body("Chỉ đánh giá sau khi đã trả xe.");
        }

        Optional<RentalRating> existing = rentalRatingRepository.findByRentalId(rentalId);
        if (existing.isPresent()) return ResponseEntity.badRequest().body("Bạn đã gửi đánh giá cho chuyến này rồi.");

        int vehicleScore = parseScore(payload.get("vehicleScore"));
        int staffScore = parseScore(payload.get("staffScore"));
        String comment = Objects.toString(payload.get("comment"), "");

        RentalRating rating = new RentalRating();
        rating.setRentalId(rentalId);
        rating.setUserId(user.getId());
        rating.setUsername(user.getUsername());
        rating.setVehicleScore(vehicleScore);
        rating.setStaffScore(staffScore);
        rating.setComment(comment);
        rating.setCreatedAt(LocalDateTime.now());

        if (record.getVehicleId() != null) {
            rating.setVehicleId(record.getVehicleId());
            vehicleRepository.findById(record.getVehicleId())
                    .ifPresent(v -> {
                        rating.setVehiclePlate(v.getPlate());
                    });
        }

        rentalRatingRepository.save(rating);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myRatings() {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(rentalRatingRepository.findAllByUserId(user.getId()));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<RentalRating>> adminRatings() {
        List<RentalRating> ratings = rentalRatingRepository.findAll();
        ratings.sort((a, b) -> Optional.ofNullable(b.getCreatedAt()).orElse(LocalDateTime.MIN)
                .compareTo(Optional.ofNullable(a.getCreatedAt()).orElse(LocalDateTime.MIN)));
        return ResponseEntity.ok(ratings);
    }

    private int parseScore(Object raw) {
        try {
            int score = Integer.parseInt(Objects.toString(raw, "0"));
            if (score < 1) return 1;
            if (score > 5) return 5;
            return score;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

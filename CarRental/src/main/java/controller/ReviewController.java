package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Review;
import CarRental.example.document.User;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.UserRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final RentalRecordRepository rentalRecordRepository;
    private final VehicleRepository vehicleRepository;
    
    public ReviewController(ReviewService reviewService, 
                            UserRepository userRepository,
                            RentalRecordRepository rentalRecordRepository,
                            VehicleRepository vehicleRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
        this.rentalRecordRepository = rentalRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }
    
    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();
        
        User user = userRepository.findByUsername(auth.getName());
        if (user == null) return ResponseEntity.status(404).body("User không tồn tại");
        
        try {
            Long bookingId = Long.parseLong((String) body.get("bookingId"));
            Long carId = Long.parseLong((String) body.get("carId"));
            Long staffId = Long.parseLong((String) body.get("staffId"));
            
            Integer carRating = null;
            Integer staffRating = null;
            
            Object carRatingObj = body.get("carRating");
            if (carRatingObj instanceof Number) {
                carRating = ((Number) carRatingObj).intValue();
            }
            
            Object staffRatingObj = body.get("staffRating");
            if (staffRatingObj instanceof Number) {
                staffRating = ((Number) staffRatingObj).intValue();
            }
            
            String comment = (String) body.get("comment");
            
            Review review = reviewService.createReview(bookingId, user.getId(), carId, staffId, carRating, staffRating, comment);
            return ResponseEntity.ok(review);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/check/{bookingId}")
    public ResponseEntity<Map<String, Boolean>> checkReviewStatus(@PathVariable("bookingId") Long bookingId) {
        boolean reviewed = reviewService.isBookingReviewed(bookingId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("reviewed", reviewed);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getReviewByBooking(@PathVariable("bookingId") Long bookingId) {
        return reviewService.getReviewByBookingId(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Admin endpoints
    @GetMapping("/admin/all")
    public ResponseEntity<List<Map<String, Object>>> getAllReviews() {
        List<Review> reviews = reviewService.getAllReviews();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Review review : reviews) {
            Map<String, Object> reviewData = new LinkedHashMap<>();
            reviewData.put("id", review.getId());
            reviewData.put("bookingId", review.getBookingId());
            reviewData.put("carRating", review.getCarRating());
            reviewData.put("staffRating", review.getStaffRating());
            reviewData.put("comment", review.getComment());
            reviewData.put("reviewDate", review.getReviewDate());
            
            // Get user info
            User user = userRepository.findById(review.getUserId()).orElse(null);
            reviewData.put("customerName", user != null ? user.getUsername() : "Unknown");
            
            // Get vehicle info
            Vehicle vehicle = vehicleRepository.findById(review.getCarId()).orElse(null);
            reviewData.put("vehicleName", vehicle != null ? (vehicle.getBrand() + " " + vehicle.getPlate()) : "Unknown");
            
            // Get staff info
            if (review.getStaffId() != null) {
                User staff = userRepository.findById(review.getStaffId()).orElse(null);
                reviewData.put("staffName", staff != null ? staff.getUsername() : "Unknown");
            } else {
                reviewData.put("staffName", "N/A");
            }
            
            result.add(reviewData);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        Map<String, Object> stats = reviewService.getReviewStats();
        return ResponseEntity.ok(stats);
    }
}

package CarRental.example.service;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Review;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewService {
    
    private static final double DECIMAL_PLACES_MULTIPLIER = 10.0;
    
    private final ReviewRepository reviewRepository;
    private final RentalRecordRepository rentalRecordRepository;
    
    public ReviewService(ReviewRepository reviewRepository, RentalRecordRepository rentalRecordRepository) {
        this.reviewRepository = reviewRepository;
        this.rentalRecordRepository = rentalRecordRepository;
    }
    
    public Review createReview(String bookingId, String userId, String carId, String staffId, 
                                Integer carRating, Integer staffRating, String comment) {
        // Check if booking exists and is completed
        RentalRecord rental = rentalRecordRepository.findById(bookingId).orElse(null);
        if (rental == null) {
            throw new IllegalArgumentException("Booking không tồn tại");
        }
        if (!"COMPLETED".equals(rental.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá khi chuyến thuê đã hoàn thành");
        }
        
        // Check if already reviewed
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new IllegalArgumentException("Chuyến thuê này đã được đánh giá");
        }
        
        // Validate ratings
        if (carRating != null && (carRating < 1 || carRating > 5)) {
            throw new IllegalArgumentException("Đánh giá xe phải từ 1 đến 5 sao");
        }
        if (staffRating != null && (staffRating < 1 || staffRating > 5)) {
            throw new IllegalArgumentException("Đánh giá nhân viên phải từ 1 đến 5 sao");
        }
        
        Review review = new Review();
        review.setBookingId(bookingId);
        review.setUserId(userId);
        review.setCarId(carId);
        review.setStaffId(staffId);
        review.setCarRating(carRating);
        review.setStaffRating(staffRating);
        review.setComment(comment);
        
        return reviewRepository.save(review);
    }
    
    public boolean isBookingReviewed(String bookingId) {
        return reviewRepository.existsByBookingId(bookingId);
    }
    
    public Optional<Review> getReviewByBookingId(String bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }
    
    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByReviewDateDesc();
    }
    
    public List<Review> getReviewsByCarId(String carId) {
        return reviewRepository.findByCarId(carId);
    }
    
    public List<Review> getReviewsByStaffId(String staffId) {
        return reviewRepository.findByStaffId(staffId);
    }
    
    public Map<String, Object> getReviewStats() {
        List<Review> allReviews = reviewRepository.findAll();
        Map<String, Object> stats = new HashMap<>();
        
        if (allReviews.isEmpty()) {
            stats.put("totalReviews", 0);
            stats.put("avgCarRating", 0.0);
            stats.put("avgStaffRating", 0.0);
            stats.put("ratingDistribution", new int[]{0, 0, 0, 0, 0});
            return stats;
        }
        
        double totalCarRating = 0;
        int carRatingCount = 0;
        double totalStaffRating = 0;
        int staffRatingCount = 0;
        int[] carRatingDistribution = new int[5]; // Index 0 = 1 star, Index 4 = 5 stars
        int[] staffRatingDistribution = new int[5];
        
        for (Review review : allReviews) {
            if (review.getCarRating() != null) {
                totalCarRating += review.getCarRating();
                carRatingCount++;
                carRatingDistribution[review.getCarRating() - 1]++;
            }
            if (review.getStaffRating() != null) {
                totalStaffRating += review.getStaffRating();
                staffRatingCount++;
                staffRatingDistribution[review.getStaffRating() - 1]++;
            }
        }
        
        stats.put("totalReviews", allReviews.size());
        stats.put("avgCarRating", carRatingCount > 0 ? Math.round((totalCarRating / carRatingCount) * DECIMAL_PLACES_MULTIPLIER) / DECIMAL_PLACES_MULTIPLIER : 0.0);
        stats.put("avgStaffRating", staffRatingCount > 0 ? Math.round((totalStaffRating / staffRatingCount) * DECIMAL_PLACES_MULTIPLIER) / DECIMAL_PLACES_MULTIPLIER : 0.0);
        stats.put("carRatingDistribution", carRatingDistribution);
        stats.put("staffRatingDistribution", staffRatingDistribution);
        
        return stats;
    }
}

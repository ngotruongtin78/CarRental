package CarRental.example.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long bookingId;
    private Long userId;
    private Long carId;
    private Long staffId;
    private Integer carRating;      // 1-5
    private Integer staffRating;    // 1-5
    private String comment;
    private LocalDateTime reviewDate;
    
    public Review() {
        this.reviewDate = LocalDateTime.now();
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
    
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    
    public Integer getCarRating() { return carRating; }
    public void setCarRating(Integer carRating) { this.carRating = carRating; }
    
    public Integer getStaffRating() { return staffRating; }
    public void setStaffRating(Integer staffRating) { this.staffRating = staffRating; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
}

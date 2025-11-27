package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "reviews")
public class Review {
    
    @Id
    private String id;
    private String bookingId;
    private String userId;
    private String carId;
    private String staffId;
    private Integer carRating;      // 1-5
    private Integer staffRating;    // 1-5
    private String comment;
    private LocalDateTime reviewDate;
    
    public Review() {
        this.reviewDate = LocalDateTime.now();
    }
    
    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    
    public Integer getCarRating() { return carRating; }
    public void setCarRating(Integer carRating) { this.carRating = carRating; }
    
    public Integer getStaffRating() { return staffRating; }
    public void setStaffRating(Integer staffRating) { this.staffRating = staffRating; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
}

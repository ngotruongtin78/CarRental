package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notifications")
public class Notification {
    
    @Id
    private String id;
    private String userId;
    private String message;
    private String type;  // SUPPORT_REPLY, BOOKING_UPDATE, etc.
    private boolean isRead;
    private LocalDateTime createdDate;
    private String supportRequestId;
    
    public Notification() {
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }
    
    public Notification(String userId, String message, String type, String supportRequestId) {
        this();
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.supportRequestId = supportRequestId;
    }
    
    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getSupportRequestId() { return supportRequestId; }
    public void setSupportRequestId(String supportRequestId) { this.supportRequestId = supportRequestId; }
}

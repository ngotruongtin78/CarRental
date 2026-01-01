package CarRental.example.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_support")
public class CustomerSupport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;     // Người gửi
    private String title;        // Tiêu đề
    private String content;      // Nội dung
    private String status;       // PENDING (Chờ xử lý), RESOLVED (Đã xong)
    private String adminReply;   // Phản hồi của Admin
    private LocalDateTime createdAt;

    public CustomerSupport() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
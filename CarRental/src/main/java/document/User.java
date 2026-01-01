package CarRental.example.document;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String role;
    private boolean enabled = true;
    private String fullName;

    private String stationId;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] licenseData;
    
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] idCardData;

    private boolean verified = false;
    private boolean verificationRequested = false;
    private boolean risk = false;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt = new Date();

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public byte[] getLicenseData() { return licenseData; }
    public void setLicenseData(byte[] licenseData) { this.licenseData = licenseData; }
    public byte[] getIdCardData() { return idCardData; }
    public void setIdCardData(byte[] idCardData) { this.idCardData = idCardData; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public boolean isVerificationRequested() { return verificationRequested; }
    public void setVerificationRequested(boolean verificationRequested) { this.verificationRequested = verificationRequested; }
    public boolean isRisk() { return risk; }
    public void setRisk(boolean risk) { this.risk = risk; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
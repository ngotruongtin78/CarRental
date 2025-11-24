package CarRental.example.document;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;
    private String password;
    private String role;
    private boolean enabled = true;

    @Field("license")
    private Binary licenseData;

    @Field("idcard")
    private Binary idCardData;

    private boolean verified = false;
    private boolean verificationRequested = false;

    private boolean risk = false;

    private Date updatedAt = new Date();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Binary getLicenseData() { return licenseData; }
    public void setLicenseData(Binary licenseData) { this.licenseData = licenseData; }

    public Binary getIdCardData() { return idCardData; }
    public void setIdCardData(Binary idCardData) { this.idCardData = idCardData; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isVerificationRequested() { return verificationRequested; }
    public void setVerificationRequested(boolean verificationRequested) { this.verificationRequested = verificationRequested; }

    public boolean isRisk() { return risk; }
    public void setRisk(boolean risk) { this.risk = risk; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
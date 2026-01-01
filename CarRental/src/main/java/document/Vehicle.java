package CarRental.example.document;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String plate;
    private String type;
    private String brand;
    private int battery;
    private double price;

    private Long stationId;
    private boolean available = true;

    private String bookingStatus = "AVAILABLE";
    private Long pendingRentalId;

    private String issue;
    private String issueSeverity;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public Long getPendingRentalId() { return pendingRentalId; }
    public void setPendingRentalId(Long pendingRentalId) { this.pendingRentalId = pendingRentalId; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getIssueSeverity() { return issueSeverity; }
    public void setIssueSeverity(String issueSeverity) { this.issueSeverity = issueSeverity; }
}

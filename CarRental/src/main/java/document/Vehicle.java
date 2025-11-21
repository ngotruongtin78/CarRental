package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;

    private String plate;
    private String type;
    private String brand;
    private int battery;
    private double price;

    private String stationId;
    private boolean available = true;

    private String bookingStatus = "AVAILABLE"; // AVAILABLE, PENDING_PAYMENT, RENTED
    private String pendingRentalId;

    private String issue;
    private String issueSeverity;


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPendingRentalId() { return pendingRentalId; }
    public void setPendingRentalId(String pendingRentalId) { this.pendingRentalId = pendingRentalId; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public String getIssueSeverity() { return issueSeverity; }
    public void setIssueSeverity(String issueSeverity) { this.issueSeverity = issueSeverity; }
}

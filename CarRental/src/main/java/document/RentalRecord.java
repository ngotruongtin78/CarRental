package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "rental_records")
public class RentalRecord {

    @Id
    private String id;
    private String username;

    private String userId;
    private String vehicleId;
    private String stationId;

    private LocalDate startDate;
    private LocalDate endDate;
    private int rentalDays;
    private double distanceKm;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private double total;
    private double damageFee;

    private String paymentMethod;
    private String status;
    private String paymentStatus;
    private LocalDateTime paidAt;

    private boolean contractSigned;
    private String checkinNotes;
    private String returnNotes;

    private boolean contractSigned;
    private String checkinNotes;
    private String returnNotes;

    public RentalRecord() {}

    public RentalRecord(String userId, String vehicleId, String stationId, double total) {
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.stationId = stationId;
        this.total = total;

        this.startTime = LocalDateTime.now();

        this.status = "PENDING";
        this.paymentStatus = "PENDING";
        this.damageFee = 0;
        this.contractSigned = false;
    }

    // GETTER – SETTER
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public void setUsername(String username) {this.username = username; }
    public String getUsername() {return username; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int rentalDays) { this.rentalDays = rentalDays; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getDamageFee() { return damageFee; }
    public void setDamageFee(double damageFee) { this.damageFee = damageFee; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public boolean isContractSigned() { return contractSigned; }
    public void setContractSigned(boolean contractSigned) { this.contractSigned = contractSigned; }

    public String getCheckinNotes() { return checkinNotes; }
    public void setCheckinNotes(String checkinNotes) { this.checkinNotes = checkinNotes; }

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}

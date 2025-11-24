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
    private LocalDateTime holdExpiresAt;

    private double total;
    private double damageFee;

    private String paymentMethod;
    private String status;
    private String paymentStatus;
    private LocalDateTime paidAt;

    private boolean contractSigned;
    private String checkinNotes;
    private Double checkinLatitude;
    private Double checkinLongitude;
    private String returnNotes;
    private Double returnLatitude;
    private Double returnLongitude;
    private byte[] checkinPhotoData;
    private byte[] returnPhotoData;
    
    private String walletReference;

    // Ảnh báo cáo sự cố (lưu base64 hoặc binary data)
    private byte[] deliveryPhotoData;

    public RentalRecord() {
    }

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


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }

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

    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

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

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public boolean isContractSigned() { return contractSigned; }
    public void setContractSigned(boolean contractSigned) { this.contractSigned = contractSigned; }

    public String getCheckinNotes() { return checkinNotes; }
    public void setCheckinNotes(String checkinNotes) { this.checkinNotes = checkinNotes; }

    public Double getCheckinLatitude() { return checkinLatitude; }
    public void setCheckinLatitude(Double checkinLatitude) { this.checkinLatitude = checkinLatitude; }

    public Double getCheckinLongitude() { return checkinLongitude; }
    public void setCheckinLongitude(Double checkinLongitude) { this.checkinLongitude = checkinLongitude; }

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }

    public Double getReturnLatitude() { return returnLatitude; }
    public void setReturnLatitude(Double returnLatitude) { this.returnLatitude = returnLatitude; }

    public Double getReturnLongitude() { return returnLongitude; }
    public void setReturnLongitude(Double returnLongitude) { this.returnLongitude = returnLongitude; }

    public byte[] getCheckinPhotoData() { return checkinPhotoData; }
    public void setCheckinPhotoData(byte[] checkinPhotoData) { this.checkinPhotoData = checkinPhotoData; }

    public byte[] getReturnPhotoData() { return returnPhotoData; }
    public void setReturnPhotoData(byte[] returnPhotoData) { this.returnPhotoData = returnPhotoData; }

    public String getWalletReference() {
        return walletReference;
    }

    public void setWalletReference(String walletReference) {
        this.walletReference = walletReference;
    }

    public byte[] getDeliveryPhotoData() {
        return deliveryPhotoData;
    }

    public void setDeliveryPhotoData(byte[] deliveryPhotoData) {
        this.deliveryPhotoData = deliveryPhotoData;
    }
}

package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "vehicle-reports")
public class VehicleReport {

    @Id
    private String id;

    private String vehicleId;
    private String vehiclePlate;
    private String staffId;
    private String staffName;
    private String stationId;

    private String issue;
    private String severity;

    private LocalDateTime reportedDate;
    private String status;

    private String notes;

    public VehicleReport() {
    }

    public VehicleReport(String vehicleId, String vehiclePlate, String staffId, String staffName,
                        String stationId, String issue, String severity) {
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate;
        this.staffId = staffId;
        this.staffName = staffName;
        this.stationId = stationId;
        this.issue = issue;
        this.severity = severity;
        this.reportedDate = LocalDateTime.now();
        this.status = "REPORTED";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(LocalDateTime reportedDate) {
        this.reportedDate = reportedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}


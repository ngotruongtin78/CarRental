package CarRental.example.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "stations")
public class Station {

    @Id
    private String id;

    private String name;
    private double latitude;
    private double longitude;
    private int availableCars;
    private String address;

    public Station() {
    }

    public Station(String name, double latitude, double longitude, int availableCars, String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.availableCars = availableCars;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getAvailableCars() {
        return availableCars;
    }

    public String getAddress() {
        return address;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setAvailableCars(int availableCars) {
        this.availableCars = availableCars;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

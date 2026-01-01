package CarRental.example.document;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "staff")
public class Staff extends User {



    public Staff() {
        super();
    }

    public Staff(String stationId) {
        this.setStationId(stationId);
    }

    public String getName() {
        return "";
    }
}
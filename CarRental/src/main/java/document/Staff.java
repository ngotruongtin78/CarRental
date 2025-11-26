package CarRental.example.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "staff")
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
package CarRental.example.repository;

import CarRental.example.document.Vehicle;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;

public class VehicleRepositoryImpl implements CarRental.example.repository.VehicleRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public VehicleRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public long updateAvailable(String id, boolean available) {

        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("available", available);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Vehicle.class);
        return result.getModifiedCount();
    }
}

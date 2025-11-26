package CarRental.example.repository;

import CarRental.example.document.CustomerSupport;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CustomerSupportRepository extends MongoRepository<CustomerSupport, String> {
    List<CustomerSupport> findByUsername(String username);
}
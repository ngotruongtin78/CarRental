package CarRental.example.repository;

import CarRental.example.document.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StaffRepository extends MongoRepository<Staff, String> {

    Staff findByUsername(String username);
}
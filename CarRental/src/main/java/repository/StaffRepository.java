package CarRental.example.repository;

import CarRental.example.document.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Staff findByUsername(String username);
}
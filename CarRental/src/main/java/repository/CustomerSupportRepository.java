package CarRental.example.repository;

import CarRental.example.document.CustomerSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerSupportRepository extends JpaRepository<CustomerSupport, Long> {
    List<CustomerSupport> findByUsername(String username);
}
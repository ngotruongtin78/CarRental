package CarRental.example.repository;

import CarRental.example.document.SupportTicket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {
    List<SupportTicket> findByUserIdOrderByUpdatedAtDesc(String userId);
}

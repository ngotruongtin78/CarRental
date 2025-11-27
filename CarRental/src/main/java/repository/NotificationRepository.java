package CarRental.example.repository;

import CarRental.example.document.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedDateDesc(String userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedDateDesc(String userId);
    long countByUserIdAndIsReadFalse(String userId);
}

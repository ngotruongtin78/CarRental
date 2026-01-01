package CarRental.example.repository;

import CarRental.example.document.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedDateDesc(String userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedDateDesc(String userId);
    long countByUserIdAndIsReadFalse(String userId);
}

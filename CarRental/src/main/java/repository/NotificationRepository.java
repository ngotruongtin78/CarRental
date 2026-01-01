package CarRental.example.repository;

import CarRental.example.document.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedDateDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedDateDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
}

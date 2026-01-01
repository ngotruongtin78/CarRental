package CarRental.example.service;

import CarRental.example.document.Notification;
import CarRental.example.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }
    
    public Notification createNotification(Long userId, String message, String type, String supportRequestId) {
        Notification notification = new Notification(userId, message, type, supportRequestId);
        return notificationRepository.save(notification);
    }
    
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }
    
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedDateDesc(userId);
    }
    
    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
    
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedDateDesc(userId);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }
}

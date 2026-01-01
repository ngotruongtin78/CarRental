package CarRental.example.controller;

import CarRental.example.document.Notification;
import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import CarRental.example.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }
    
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();
        
        User user = userRepository.findByUsername(auth.getName());
        if (user == null) return ResponseEntity.status(404).build();
        
        List<Notification> notifications = notificationService.getNotificationsForUser(user.getId());
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();
        
        User user = userRepository.findByUsername(auth.getName());
        if (user == null) return ResponseEntity.status(404).build();
        
        long count = notificationService.getUnreadCountForUser(user.getId());
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/mark-read/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();
        
        notificationService.markAsRead(id);
        return ResponseEntity.ok("OK");
    }
    
    @PostMapping("/mark-all-read")
    public ResponseEntity<String> markAllAsRead() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();
        
        User user = userRepository.findByUsername(auth.getName());
        if (user == null) return ResponseEntity.status(404).build();
        
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok("OK");
    }
}

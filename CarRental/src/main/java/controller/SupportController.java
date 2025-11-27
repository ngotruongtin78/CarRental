package CarRental.example.controller;

import CarRental.example.document.SupportTicket;
import CarRental.example.document.User;
import CarRental.example.repository.SupportTicketRepository;
import CarRental.example.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SupportController {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;

    public SupportController(SupportTicketRepository supportTicketRepository, UserRepository userRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName());
    }

    @PostMapping("/support/tickets")
    public ResponseEntity<?> submitTicket(@RequestBody Map<String, String> payload) {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        String category = Optional.ofNullable(payload.get("category")).orElse("SUPPORT");
        String content = Optional.ofNullable(payload.get("content")).orElse("").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập nội dung cần hỗ trợ hoặc khiếu nại.");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setUserId(user.getId());
        ticket.setUsername(user.getUsername());
        ticket.setCategory(category);
        ticket.setContent(content);
        ticket.setStatus("OPEN");
        ticket.setSeenByUser(true);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        supportTicketRepository.save(ticket);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/support/tickets/my")
    public ResponseEntity<?> myTickets() {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");
        List<SupportTicket> tickets = supportTicketRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/support/tickets/{id}/ack")
    public ResponseEntity<?> acknowledge(@PathVariable("id") String id) {
        User user = currentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<SupportTicket> ticketOpt = supportTicketRepository.findById(id);
        if (ticketOpt.isEmpty()) return ResponseEntity.status(404).body("Ticket not found");
        SupportTicket ticket = ticketOpt.get();
        if (!user.getId().equals(ticket.getUserId())) return ResponseEntity.status(403).body("Forbidden");

        ticket.setSeenByUser(true);
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);
        return ResponseEntity.ok(Collections.singletonMap("status", "ACKED"));
    }

    @GetMapping("/admin/support")
    public ResponseEntity<List<SupportTicket>> allTickets() {
        List<SupportTicket> tickets = supportTicketRepository.findAll();
        tickets.sort((a, b) -> Optional.ofNullable(b.getUpdatedAt()).orElse(LocalDateTime.MIN)
                .compareTo(Optional.ofNullable(a.getUpdatedAt()).orElse(LocalDateTime.MIN)));
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/admin/support/{id}/reply")
    public ResponseEntity<?> reply(@PathVariable("id") String id, @RequestBody Map<String, String> payload) {
        String reply = Optional.ofNullable(payload.get("reply")).orElse("").trim();
        Optional<SupportTicket> ticketOpt = supportTicketRepository.findById(id);
        if (ticketOpt.isEmpty()) return ResponseEntity.status(404).body("Ticket not found");

        SupportTicket ticket = ticketOpt.get();
        ticket.setAdminReply(reply);
        ticket.setStatus("RESPONDED");
        ticket.setSeenByUser(false);
        ticket.setRespondedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        return ResponseEntity.ok(ticket);
    }
}

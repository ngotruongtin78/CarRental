package CarRental.example.controller;

import CarRental.example.document.CustomerSupport;
import CarRental.example.repository.CustomerSupportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class CustomerSupportController {

    private final CustomerSupportRepository supportRepo;

    public CustomerSupportController(CustomerSupportRepository supportRepo) {
        this.supportRepo = supportRepo;
    }

    // 1. Khách hàng gửi yêu cầu
    @PostMapping("/create")
    public ResponseEntity<?> createTicket(@RequestBody Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();

        CustomerSupport ticket = new CustomerSupport();
        ticket.setUsername(auth.getName());
        ticket.setTitle(body.get("title"));
        ticket.setContent(body.get("content"));

        supportRepo.save(ticket);
        return ResponseEntity.ok("Gửi thành công");
    }

    // 2. Khách hàng xem lịch sử yêu cầu của mình
    @GetMapping("/my-history")
    public ResponseEntity<List<CustomerSupport>> getMyHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return ResponseEntity.status(401).build();

        List<CustomerSupport> list = supportRepo.findByUsername(auth.getName());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())); // Mới nhất lên đầu

        return ResponseEntity.ok(list);
    }

    // 3. Admin lấy tất cả yêu cầu
    @GetMapping("/admin/all")
    public ResponseEntity<List<CustomerSupport>> getAllTickets() {
        List<CustomerSupport> list = supportRepo.findAll();
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return ResponseEntity.ok(list);
    }

    // 4. Admin phản hồi
    @PostMapping("/admin/reply/{id}")
    public ResponseEntity<?> replyTicket(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        CustomerSupport ticket = supportRepo.findById(id).orElse(null);
        if (ticket == null) return ResponseEntity.notFound().build();

        ticket.setAdminReply(body.get("reply"));
        ticket.setStatus("RESOLVED");
        supportRepo.save(ticket);
        return ResponseEntity.ok("Đã phản hồi");
    }
}
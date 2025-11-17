package CarRental.example.controller;

import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.document.RentalRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;
import java.net.URI;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    private final RentalRecordRepository rentalRepo;

    public PaymentController(RentalRecordRepository rentalRepo) {
        this.rentalRepo = rentalRepo;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String,Object> req) {
        try {
            RestTemplate rest = new RestTemplate();

            Map<String,Object> body = new HashMap<>();
            body.put("orderCode", req.get("orderCode"));
            body.put("amount", req.get("amount"));
            body.put("description", req.get("description"));
            body.put("returnUrl", "http://localhost:8080/payment/success");
            body.put("cancelUrl", "http://localhost:8080/user-datxe");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.set("x-checksum-key", checksumKey);

            HttpEntity<Map<String,Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = rest.exchange(
                    "https://api.payos.vn/v2/payment-requests",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<?> paymentSuccess(@RequestParam Map<String, String> params) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            String vehicleId = params.get("vehicleId");
            String stationId = params.get("stationId");
            int price = Integer.parseInt(params.get("amount"));

            RentalRecord record = new RentalRecord();
            record.setId(username);
            record.setVehicleId(vehicleId);
            record.setStationId(stationId);
            record.setTotal(price);
            record.setStartTime(LocalDateTime.now());

            rentalRepo.save(record);

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/lichsuthue"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Payment success but error: " + e.getMessage());
        }
    }
}

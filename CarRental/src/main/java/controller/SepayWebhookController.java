package CarRental.example.controller;

import CarRental.example.service.sepay.SepayWebhookData;
import CarRental.example.service.sepay.SepayWebhookHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sepay")
public class SepayWebhookController {

    private final SepayWebhookHandler webhookHandler;

    public SepayWebhookController(SepayWebhookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> webhook(@RequestBody SepayWebhookData payload) {
        return webhookHandler.processWebhook(payload);
    }
}

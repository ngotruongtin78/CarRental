package CarRental.example.controller;

import CarRental.example.service.RentalRecordService;
import CarRental.example.document.RentalRecord;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payos")
public class PayOSController {

    private final RentalRecordService historyService;

    public PayOSController(RentalRecordService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/success")
    public String paymentSuccess(@RequestBody RentalRecord record) {
        record.setPaymentStatus("PAID");
        historyService.saveRecord(record);
        return "OK";
    }

    @PostMapping("/cancel")
    public String paymentCancel(@RequestBody RentalRecord record) {
        record.setPaymentStatus("FAILED");
        historyService.saveRecord(record);
        return "CANCELLED";
    }
}

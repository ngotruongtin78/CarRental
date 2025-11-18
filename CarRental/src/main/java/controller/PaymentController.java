package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.VehicleService;
import CarRental.example.service.sepay.SepayQRData;
import CarRental.example.service.sepay.SepayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final String accountName;
    private final String accountNumber;
    private final String bankName;

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final SepayService sepayService;

    public PaymentController(RentalRecordRepository rentalRepo,
                             VehicleRepository vehicleRepository,
                             VehicleService vehicleService,
                             SepayService sepayService,
                             @Value("${sepay.account-name:}") String accountName,
                             @Value("${sepay.account-number:}") String accountNumber,
                             @Value("${sepay.bank-name:}") String bankName) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.sepayService = sepayService;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
    }

    private boolean expireIfNeeded(RentalRecord record) {
        if (record == null) return false;

        boolean pending = "PENDING_PAYMENT".equalsIgnoreCase(record.getStatus());
        boolean expired = record.getHoldExpiresAt() != null && LocalDateTime.now().isAfter(record.getHoldExpiresAt());
        if (pending && expired) {
            record.setStatus("CANCELLED");
            record.setPaymentStatus("EXPIRED");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.releaseHold(record.getVehicleId(), record.getId());
            return true;
        }
        return false;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thanh toán");
        }
        String rentalId = (String) req.get("rentalId");
        if (rentalId == null || rentalId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu mã chuyến thuê");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
        }

        if (expireIfNeeded(record)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body("Đơn đặt đã hết hạn thanh toán. Vui lòng đặt xe lại.");
        }

        if (record.getPaymentStatus() != null && "PAID".equalsIgnoreCase(record.getPaymentStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chuyến thuê đã thanh toán");
        }

        Vehicle vehicle = vehicleRepository.findById(record.getVehicleId()).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Xe không tồn tại");
        }

        int rentalDays = record.getRentalDays();
        if (rentalDays <= 0) {
            if (record.getStartDate() != null && record.getEndDate() != null) {
                rentalDays = (int) Math.max(1, ChronoUnit.DAYS.between(record.getStartDate(), record.getEndDate()));
            } else {
                rentalDays = 1;
            }
        }
        double amount = record.getTotal() > 0 ? record.getTotal() : rentalDays * vehicle.getPrice();
        record.setTotal(amount);
        record.setPaymentMethod("bank_transfer");
        record.setPaymentStatus("PENDING");
        record.setStatus("PENDING_PAYMENT");
        record.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        rentalRepo.save(record);
        vehicleService.markPendingPayment(record.getVehicleId(), rentalId);

        Map<String, Object> payload = new LinkedHashMap<>();
        int amountInt = (int) Math.round(amount);
        String description = "Thanh toan don hang #" + rentalId;

        try {
            SepayQRData qrData = sepayService.createPaymentQR(amountInt, description);
            payload.put("amount", amountInt);
            payload.put("description", description);
            payload.put("qrUrl", qrData.getQrUrl());
            payload.put("qrBase64", qrData.getQr());
            payload.put("bank", Optional.ofNullable(qrData.getBank()).orElse(bankName));
            payload.put("accountName", Optional.ofNullable(qrData.getAccountName()).orElse(accountName));
            payload.put("accountNumber", Optional.ofNullable(qrData.getAccountNumber()).orElse(accountNumber));
            payload.put("rentalId", rentalId);
            payload.put("status", "OK");
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
        }

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/return")
    public RedirectView paymentReturn(@RequestParam("rentalId") String rentalId,
                                      @RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "amount", required = false) String amount) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            if (expireIfNeeded(record)) {
                RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&cancel=1");
                redirectView.setExposeModelAttributes(false);
                return redirectView;
            }
            record.setPaymentStatus("PAID");
            if (amount != null) {
                try { record.setTotal(Double.parseDouble(amount)); } catch (NumberFormatException ignored) {}
            }
            record.setPaidAt(LocalDateTime.now());
            record.setStatus("PAID");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.markRented(record.getVehicleId(), rentalId);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&success=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    @GetMapping("/cancel")
    public RedirectView paymentCancel(@RequestParam("rentalId") String rentalId) {
        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            record.setPaymentStatus("CANCELLED");
            record.setStatus("CANCELLED");
            record.setHoldExpiresAt(null);
            rentalRepo.save(record);
            vehicleService.releaseHold(record.getVehicleId(), rentalId);
        }
        RedirectView redirectView = new RedirectView("/thanhtoan?rentalId=" + rentalId + "&cancel=1");
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> paymentWebhook(@RequestBody Map<String, Object> payload) {
        String description = Objects.toString(payload.get("description"), "");
        String rentalId = null;
        if (description.contains("#")) {
            rentalId = description.substring(description.indexOf('#') + 1).trim();
        } else if (!description.isBlank()) {
            rentalId = description
                    .replace("Thanh toan don hang", "")
                    .replace("Thanh toan don", "")
                    .trim();
        }

        if (rentalId == null || rentalId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu rentalId trong description");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
        }

        if (!expireIfNeeded(record)) {
            record.setPaymentStatus("PAID");
            record.setPaymentMethod("bank_transfer");
            record.setStatus("PAID");
            record.setHoldExpiresAt(null);
            if (payload.get("amount") instanceof Number amount) {
                record.setTotal(amount.doubleValue());
            }
            record.setPaidAt(LocalDateTime.now());
            rentalRepo.save(record);
            vehicleService.markRented(record.getVehicleId(), rentalId);
        }

        return ResponseEntity.ok("OK");
    }
}

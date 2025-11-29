package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.document.Vehicle;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.repository.VehicleRepository;
import CarRental.example.service.VehicleService;
import CarRental.example.service.sepay.SepayWebhookData;
import CarRental.example.service.sepay.SepayWebhookHandler;
import CarRental.example.service.sepay.SepayQrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping({"/payment", "/api/payment"})
public class PaymentController {

    private final String accountName;
    private final String accountNumber;
    private final String bankName;

    private final RentalRecordRepository rentalRepo;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final SepayQrService qrService;
    private final SepayWebhookHandler webhookHandler;

    public PaymentController(RentalRecordRepository rentalRepo,
                             VehicleRepository vehicleRepository,
                             VehicleService vehicleService,
                             SepayQrService qrService,
                             SepayWebhookHandler webhookHandler,
                             @Value("${sepay.account-name:}") String accountName,
                             @Value("${sepay.account-number:}") String accountNumber,
                             @Value("${sepay.bank-name:}") String bankName) {
        this.rentalRepo = rentalRepo;
        this.vehicleRepository = vehicleRepository;
        this.vehicleService = vehicleService;
        this.qrService = qrService;
        this.webhookHandler = webhookHandler;
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody(required = false) Map<String, Object> req,
                                         @RequestParam(value = "rentalId", required = false) String rentalIdParam,
                                         @RequestParam(value = "deposit", required = false) String depositQuery) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thanh toán");
        }

        String rentalId = null;
        if (req != null && req.get("rentalId") instanceof String) {
            rentalId = (String) req.get("rentalId");
        }
        if (rentalId == null || rentalId.isEmpty()) {
            rentalId = rentalIdParam;
        }
        if (rentalId == null || rentalId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu mã chuyến thuê");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !Objects.equals(record.getUsername(), username)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
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
        if (!"cash".equalsIgnoreCase(record.getPaymentMethod())) {
            record.setPaymentMethod("bank_transfer");
        }

        double depositPaid = Optional.ofNullable(record.getDepositPaidAmount()).orElse(0.0);
        boolean cashFlow = "cash".equalsIgnoreCase(record.getPaymentMethod());

        boolean depositRequested = false;
        if (depositQuery != null) {
            depositRequested = "true".equalsIgnoreCase(depositQuery)
                    || "1".equals(depositQuery)
                    || "yes".equalsIgnoreCase(depositQuery);
        }

        if (!depositRequested && req != null) {
            Object depositFlag = req.get("deposit");
            depositRequested = Boolean.TRUE.equals(depositFlag)
                    || "true".equalsIgnoreCase(String.valueOf(depositFlag))
                    || "1".equals(String.valueOf(depositFlag))
                    || "yes".equalsIgnoreCase(String.valueOf(depositFlag));
        }

        if (!depositRequested && cashFlow
                && "DEPOSIT_PENDING".equalsIgnoreCase(record.getPaymentStatus())) {
            depositRequested = true;
        }
        double depositRequired = cashFlow
                ? Optional.ofNullable(record.getDepositRequiredAmount()).orElse(Math.round(amount * 0.3 * 100.0) / 100.0)
                : 0.0;

        double amountToCollect;
        double depositRemaining = 0.0;
        boolean isDepositOrder = false;
        
        // ===== TÍNH LẠI THỜI GIỮ CHỖ KHI VÀO LẠI =====
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newHoldExpiry;

        if (cashFlow && depositRequested && depositPaid < depositRequired) {
            // Đặt cọc 30% → Giữ 8 tiếng từ BÂY GIỜ
            record.setPaymentStatus("DEPOSIT_PENDING");
            record.setStatus("PENDING_PAYMENT");
            newHoldExpiry = now.plusHours(8);
            record.setHoldExpiresAt(newHoldExpiry);
            
            amountToCollect = depositRequired - depositPaid;
            record.setDepositRequiredAmount(depositRequired);
            depositRemaining = amountToCollect;
            isDepositOrder = true;
        } else {
            // Chuyển khoản 100% → Giữ 24 tiếng từ BÂY GIỜ
            amountToCollect = Math.max(0, amount - depositPaid);
            record.setPaymentStatus("PENDING");
            record.setStatus("PENDING_PAYMENT");
            newHoldExpiry = now.plusHours(24);
            record.setHoldExpiresAt(newHoldExpiry);
        }

        if (amountToCollect <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không còn khoản cần thanh toán");
        }

        rentalRepo.save(record);
        vehicleService.markPendingPayment(record.getVehicleId(), rentalId);

        // ==== TẠO QR ====
        int amountInt = (int) Math.round(amountToCollect);
        String qrUrl = qrService.generateQrUrl(rentalId, amountInt, isDepositOrder);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountInt);
        payload.put("description", isDepositOrder ? "deposit" + rentalId : rentalId);

        payload.put("qrUrl", qrUrl);
        payload.put("qrBase64", null);
        payload.put("bank", bankName);
        payload.put("accountName", accountName);
        payload.put("accountNumber", accountNumber);
        payload.put("rentalId", rentalId);
        payload.put("status", "OK");
        payload.put("depositPending", cashFlow && amountToCollect > 0);
        payload.put("depositRequired", depositRequired);
        payload.put("depositPaid", depositPaid);
        payload.put("depositRemaining", depositRemaining > 0 ? depositRemaining : amountToCollect);
        payload.put("paymentMethod", record.getPaymentMethod());
        payload.put("paymentStatus", record.getPaymentStatus());

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/create-qr")
    public ResponseEntity<?> createQr(@RequestParam int amount,
                                      @RequestParam String description,
                                      @RequestParam(required = false) String orderId,
                                      @RequestParam(value = "deposit", required = false) Boolean deposit) {
        if (amount <= 0 || description.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Thiếu số tiền hoặc mô tả thanh toán");
        }

        String qrUrl = qrService.generateQrUrl(orderId != null ? orderId : "ORDER", amount,
                deposit != null && deposit);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("amount", amount);
        resp.put("description", description);
        resp.put("qrUrl", qrUrl);
        resp.put("qrBase64", null);
        resp.put("bank", bankName);
        resp.put("accountName", accountName);
        resp.put("accountNumber", accountNumber);
        resp.put("orderId", orderId);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/return")
    public RedirectView paymentReturn(@RequestParam("rentalId") String rentalId,
                                      @RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "amount", required = false) String amount) {

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record != null) {
            double paidAmount = 0;
            if (amount != null) {
                try {
                    paidAmount = Double.parseDouble(amount);
                } catch (NumberFormatException ignored) {}
            }

            if ("cash".equalsIgnoreCase(record.getPaymentMethod())) {
                double depositPaid = Optional.ofNullable(record.getDepositPaidAmount()).orElse(0.0);
                double newPaid = depositPaid + paidAmount;
                record.setDepositPaidAmount(newPaid);
                if (paidAmount > 0) {
                    record.setDepositPaidAt(LocalDateTime.now());
                }
                double depositRequired = Optional.ofNullable(record.getDepositRequiredAmount())
                        .orElse(Math.round(record.getTotal() * 0.3 * 100.0) / 100.0);

                if (newPaid >= record.getTotal()) {
                    record.setPaymentStatus("PAID");
                    record.setStatus("PAID");
                    // Đã thanh toán 100% → giữ đến thời gian thuê (startTime)
                    record.setHoldExpiresAt(record.getStartTime());
                    record.setPaidAt(LocalDateTime.now());
                    rentalRepo.save(record);
                    vehicleService.markRented(record.getVehicleId(), rentalId);
                } else if (newPaid >= depositRequired) {
                    record.setPaymentStatus("PAY_AT_STATION");
                    record.setStatus("ACTIVE");
                    // Đã đặt cọc 30% → giữ 8 tiếng
                    record.setHoldExpiresAt(LocalDateTime.now().plusHours(8));
                    rentalRepo.save(record);
                    vehicleService.markDeposited(record.getVehicleId(), rentalId);
                } else {
                    record.setPaymentStatus("DEPOSIT_PENDING");
                    rentalRepo.save(record);
                }
            } else {
                record.setPaymentStatus("PAID");
                if (paidAmount > 0) {
                    record.setTotal(paidAmount);
                }
                record.setPaidAt(LocalDateTime.now());
                record.setStatus("PAID");
                // Đã thanh toán 100% → giữ đến thời gian thuê (startTime)
                record.setHoldExpiresAt(record.getStartTime());
                rentalRepo.save(record);
                vehicleService.markRented(record.getVehicleId(), rentalId);
            }
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
    public ResponseEntity<String> paymentWebhook(@RequestBody SepayWebhookData payload) {
        return webhookHandler.processWebhook(payload);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkPayment(@RequestParam("rentalId") String rentalId) {
        Map<String, Object> resp = new HashMap<>();

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null) {
            resp.put("paid", false);
            resp.put("depositPaid", false);
            resp.put("message", "Không tìm thấy chuyến thuê");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        }

        boolean paid = "PAID".equalsIgnoreCase(record.getPaymentStatus());
        double depositPaidAmount = Optional.ofNullable(record.getDepositPaidAmount()).orElse(0.0);
        double depositRequired = Optional.ofNullable(record.getDepositRequiredAmount())
                .orElseGet(() -> "cash".equalsIgnoreCase(record.getPaymentMethod())
                        ? Math.round(record.getTotal() * 0.3 * 100.0) / 100.0
                        : 0.0);

        if ("cash".equalsIgnoreCase(record.getPaymentMethod()) && record.getDepositRequiredAmount() == null) {
            record.setDepositRequiredAmount(depositRequired);
            rentalRepo.save(record);
        }

        boolean depositSatisfied = paid
                || "PAY_AT_STATION".equalsIgnoreCase(record.getPaymentStatus())
                || (depositRequired > 0 && depositPaidAmount >= depositRequired);

        resp.put("paid", paid);
        resp.put("depositPaid", depositSatisfied);
        resp.put("status", record.getPaymentStatus());
        resp.put("rentalId", rentalId);
        resp.put("depositRequired", depositRequired);
        resp.put("depositPaidAmount", depositPaidAmount);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/extra-fee/create-order")
    public ResponseEntity<?> createExtraFeeOrder(@RequestBody(required = false) Map<String, Object> req,
                                                 @RequestParam(value = "rentalId", required = false) String rentalIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để thanh toán");
        }

        String rentalId = null;
        if (req != null && req.get("rentalId") instanceof String) rentalId = (String) req.get("rentalId");
        if (rentalId == null || rentalId.isBlank()) rentalId = rentalIdParam;
        if (rentalId == null || rentalId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu mã chuyến thuê");
        }

        RentalRecord record = rentalRepo.findById(rentalId).orElse(null);
        if (record == null || !username.equals(record.getUsername())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy chuyến thuê");
        }

        double extraAmount = record.getAdditionalFeeAmount() != null
                ? record.getAdditionalFeeAmount()
                : record.getDamageFee();
        double extraPaid = Optional.ofNullable(record.getAdditionalFeePaidAmount()).orElse(0.0);
        if (extraAmount <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không có phí phát sinh cần thanh toán");
        }

        double outstanding = Math.max(0, extraAmount - extraPaid);
        if (outstanding <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Bạn đã thanh toán đủ phí phát sinh");
        }

        int amountInt = (int) Math.round(outstanding);
        String qrUrl = qrService.generateIncidentQrUrl(rentalId, amountInt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountInt);
        payload.put("description", "incident" + rentalId);
        payload.put("qrUrl", qrUrl);
        payload.put("qrBase64", null);
        payload.put("bank", bankName);
        payload.put("accountName", accountName);
        payload.put("accountNumber", accountNumber);
        payload.put("rentalId", rentalId);
        payload.put("note", record.getAdditionalFeeNote() != null ? record.getAdditionalFeeNote() : record.getReturnNotes());
        payload.put("outstanding", outstanding);

        return ResponseEntity.ok(payload);
    }
}

package CarRental.example.controller;

import CarRental.example.document.RentalRecord;
import CarRental.example.repository.RentalRecordRepository;
import CarRental.example.service.sepay.SepayQRData;
import CarRental.example.service.sepay.SepayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SepayQRPageController {

    private final SepayService sepayService;
    private final RentalRecordRepository rentalRecordRepository;

    public SepayQRPageController(SepayService sepayService,
                                 RentalRecordRepository rentalRecordRepository) {
        this.sepayService = sepayService;
        this.rentalRecordRepository = rentalRecordRepository;
    }

    @GetMapping("/sepay-qr-pay")
    public String showQr(@RequestParam("rentalId") String rentalId, Model model) {

        RentalRecord rental = rentalRecordRepository.findById(rentalId).orElse(null);
        if (rental == null) {
            model.addAttribute("error", "Không tìm thấy đơn thuê");
            return "sepay-qr";
        }

        int amount = (int) rental.getTotal();

        SepayQRData qr = sepayService.createQR(amount, rentalId);

        model.addAttribute("qrUrl", qr.getQrUrl());
        model.addAttribute("amount", qr.getAmount());
        model.addAttribute("accountName", qr.getAccountName());
        model.addAttribute("accountNumber", qr.getAccountNumber());
        model.addAttribute("orderId", rentalId);

        return "sepay-qr";
    }
}

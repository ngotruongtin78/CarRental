package CarRental.example.service.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SepayQrService {

    @Value("${sepay.account-number:}")
    private String accountNumber;

    @Value("${sepay.bank-name:}")
    private String bankName;

    @Value("${sepay.account-name:}")
    private String accountName;

    public String generateQrUrl(String rentalId, int amount) {
        return generateQrUrl(rentalId, amount, false);
    }

    public String generateQrUrl(String rentalId, int amount, boolean deposit) {

        String cleanedRentalId = rentalId.toLowerCase().replace("rental", "").trim();

        String prefix = deposit ? "depositrental" : "rental";

        String description = prefix + cleanedRentalId;

        String encodedDes = URLEncoder.encode(description, StandardCharsets.UTF_8);

        return String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                accountNumber,
                bankName,
                amount,
                encodedDes
        );
    }


    public String getAccountName() { return accountName; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankName() { return bankName; }
}

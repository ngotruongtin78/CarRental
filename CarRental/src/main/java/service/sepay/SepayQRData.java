package CarRental.example.service.sepay;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SepayQRData {

    @JsonProperty("qr")
    private String qr;

    @JsonProperty("qr_url")
    private String qrUrl;

    @JsonProperty("bank")
    private String bank;

    @JsonProperty("account_name")
    private String accountName;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("amount")
    private int amount;

    public String getQr() {
        return qr;
    }

    public void setQr(String qr) {
        this.qr = qr;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}

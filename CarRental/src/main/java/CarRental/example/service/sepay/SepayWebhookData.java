package CarRental.example.service.sepay;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SepayWebhookData {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("amount")
    private int amount;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("bank_code")
    private String bankCode;

    @JsonProperty("description")
    private String description;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("status")
    private String status;   // SUCCESS | FAILED

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

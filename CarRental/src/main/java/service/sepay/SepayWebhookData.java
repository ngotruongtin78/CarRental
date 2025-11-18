package CarRental.example.service.sepay;

import lombok.Data;

@Data
public class SepayWebhookData {

    private String tranId;
    private String account_number;
    private String bank_name;
    private String amount;
    private String sub_amount;
    private String content;
    private String description;
    private String reference_number;
    private String transaction_date;
    private String transfer_type;
    private String paymentStatus;

}

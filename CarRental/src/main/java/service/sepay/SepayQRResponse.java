package CarRental.example.service.sepay;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SepayQRResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private SepayQRData data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SepayQRData getData() {
        return data;
    }

    public void setData(SepayQRData data) {
        this.data = data;
    }
}

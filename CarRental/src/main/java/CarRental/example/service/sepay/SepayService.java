package CarRental.example.service.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SepayService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String accountNumber;
    private final String endpoint;

    public SepayService(RestTemplate restTemplate,
                        @Value("${sepay.api-key:}") String apiKey,
                        @Value("${sepay.account-number:}") String accountNumber,
                        @Value("${sepay.endpoint:https://api.sepay.vn/v1/transactions/generate}") String endpoint) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.accountNumber = accountNumber;
        this.endpoint = endpoint;
    }

    /**
     * Gọi API SePay để tạo mã QR chuyển khoản theo hướng dẫn chuẩn.
     */
    public SepayQRData generateQR(int amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ");
        }
        if (apiKey == null || apiKey.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình SePay (api-key/account-number)");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("account_number", accountNumber);
        body.put("amount", amount);
        body.put("description", description);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SepayQRResponse> response =
                    restTemplate.exchange(endpoint, HttpMethod.POST, entity, SepayQRResponse.class);

            SepayQRResponse payload = response.getBody();
            if (payload == null) {
                throw new IllegalStateException("Không nhận được phản hồi từ SePay");
            }

            if (!"success".equalsIgnoreCase(payload.getStatus()) || payload.getData() == null) {
                String msg = payload.getMessage();
                if (msg == null || msg.isBlank()) {
                    msg = "SePay không trả về mã QR";
                }
                throw new IllegalStateException(msg);
            }

            return payload.getData();
        } catch (HttpStatusCodeException ex) {
            String detail = ex.getResponseBodyAsString();
            String errorMsg = "SePay trả về lỗi: " + ex.getStatusCode();
            if (detail != null && !detail.isBlank()) {
                errorMsg += " - " + detail;
            }
            throw new IllegalStateException(errorMsg, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Gọi SePay thất bại: " + ex.getMessage(), ex);
        }
    }

    /**
     * Giữ lại tên hàm cũ để tương thích với mã đã gọi trước đây.
     */
    public SepayQRData createPaymentQR(int amount, String description) {
        return generateQR(amount, description);
    }
}

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

    public SepayQRData createPaymentQR(int amount, String description) {
        if (apiKey == null || apiKey.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình SePay (api-key/account-number)");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("amount", amount);
        request.put("account_number", accountNumber);
        request.put("description", description);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<SepayQRResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    SepayQRResponse.class
            );

            SepayQRResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Không nhận được phản hồi từ SePay");
            }

            if (!"success".equalsIgnoreCase(body.getStatus()) || body.getData() == null) {
                String message = body.getMessage();
                if (message == null || message.isBlank()) {
                    message = "SePay không trả về mã QR";
                }
                throw new IllegalStateException(message);
            }

            return body.getData();
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
}

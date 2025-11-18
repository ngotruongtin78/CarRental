package CarRental.example.service.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class SepayService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String accountNumber;
    private final String endpoint;

    public SepayService(RestTemplateBuilder builder,
                        @Value("${sepay.api-key:}") String apiKey,
                        @Value("${sepay.account-number:}") String accountNumber,
                        @Value("${sepay.endpoint:https://api.sepay.vn/v1/transactions/generate}") String endpoint) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
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

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("SePay trả về lỗi HTTP " + response.getStatusCode());
            }

            SepayQRResponse body = response.getBody();
            if (body == null || body.getData() == null) {
                throw new IllegalStateException("Không nhận được dữ liệu QR từ SePay");
            }
            return body.getData();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Gọi SePay thất bại: " + ex.getMessage(), ex);
        }
    }
}

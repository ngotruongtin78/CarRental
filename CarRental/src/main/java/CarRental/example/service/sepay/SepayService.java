package CarRental.example.service.sepay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SepayService {

    @Value("${sepay.api-url:https://api.sepay.vn}")
    private String apiBaseUrl;

    @Value("${sepay.api-key:}")
    private String apiKey;

    @Value("${sepay.merchant-code:}")
    private String merchantCode;

    @Value("${sepay.secret-key:}")
    private String secretKey;

    // URL webhook public – bạn đã dùng ngrok:
    // ví dụ: https://willfully-nonarsenic-halina.ngrok-free.dev/payment/webhook
    @Value("${sepay.callback-url:}")
    private String callbackUrl;

    private final RestTemplate restTemplate;

    public SepayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SepayQRData createQR(int amount, String description, String orderId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Mô tả thanh toán không được để trống");
        }
        if (merchantCode == null || merchantCode.isBlank()
                || secretKey == null || secretKey.isBlank()
                || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Cấu hình SePay (merchantCode / secretKey / apiKey) chưa đầy đủ");
        }

        if (!StringUtils.hasText(orderId)) {
            orderId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        } else if (orderId.length() > 32) {
            orderId = orderId.substring(0, 32);
        }
        String signature = SepaySignatureUtil.generateSignature(orderId, amount, secretKey);
        if (signature == null) {
            throw new IllegalStateException("Không thể tạo chữ ký SePay");
        }

        // Build body JSON đúng theo API SePay
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("order_id", orderId);
        body.put("merchant_code", merchantCode);
        body.put("description", description);
        body.put("signature", signature);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            body.put("callback_url", callbackUrl);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // API v2 dùng Bearer api_key
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = apiBaseUrl + "/v2/qr/generate";

        ResponseEntity<SepayQRResponse> responseEntity =
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, SepayQRResponse.class);

        if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new IllegalStateException("Không nhận được phản hồi hợp lệ từ SePay");
        }

        SepayQRResponse response = responseEntity.getBody();

        if (response.getStatus() == null || !"success".equalsIgnoreCase(response.getStatus())) {
            String msg = response.getMessage() != null ? response.getMessage() : "Không rõ lỗi";
            throw new IllegalStateException("SePay trả về lỗi: " + msg);
        }

        SepayQRData data = response.getData();
        if (data == null || data.getQrUrl() == null || data.getQrUrl().isBlank()) {
            throw new IllegalStateException("SePay không trả về dữ liệu QR hợp lệ");
        }

        return data;
    }

}

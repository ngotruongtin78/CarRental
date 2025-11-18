package CarRental.example.service.sepay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SepayQRData createQR(int amount, String description) {
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

        // tạo orderId ngẫu nhiên
        String orderId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // signature = sha256(merchantCode|orderId|amount|secretKey)
        String rawSignature = merchantCode + "|" + orderId + "|" + amount + "|" + secretKey;
        String signature = sha256(rawSignature);

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

        // Ở đây dùng status thay cho code
        if (response.isStatus() == null || !"success".equalsIgnoreCase(response.isStatus())) {
            String msg = response.getMessage() != null ? response.getMessage() : "Không rõ lỗi";
            throw new IllegalStateException("SePay trả về lỗi: " + msg);
        }

        SepayQRData data = response.getData();
        if (data == null || data.getQrUrl() == null || data.getQrUrl().isBlank()) {
            throw new IllegalStateException("SePay không trả về dữ liệu QR hợp lệ");
        }

        return data;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký SHA-256", ex);
        }
    }
}

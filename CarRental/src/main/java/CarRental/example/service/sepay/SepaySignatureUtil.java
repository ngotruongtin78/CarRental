package CarRental.example.service.sepay;

import java.security.MessageDigest;

public class SepaySignatureUtil {

    /**
     * Tạo lại chữ ký SHA256
     */
    public static String generateSignature(String orderId, int amount, String secretKey) {
        try {
            String raw = orderId + amount + secretKey;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * So sánh signature webhook và signature sinh ra
     */
    public static boolean verifySignature(SepayWebhookData data, String secretKey) {
        String signature = generateSignature(
                data.getOrderId(),
                data.getAmount(),
                secretKey
        );

        return signature != null && signature.equalsIgnoreCase(data.getSignature());
    }
}

package iuh.fit.se.sebook_backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class để gửi email sử dụng Brevo API
 */
@Service
public class EmailSenderUtil {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderUtil.class);

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    private final RestTemplate restTemplate;

    public EmailSenderUtil() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Gửi email sử dụng Brevo API
     *
     * @param toEmail Email người nhận
     * @param subject Tiêu đề email
     * @param body    Nội dung email (HTML)
     */
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            // Tạo request body theo format Brevo API
            Map<String, Object> requestBody = new HashMap<>();
            
            // Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("email", senderEmail);
            requestBody.put("sender", sender);

            // Recipient - Brevo yêu cầu array of objects
            Map<String, String> toRecipient = new HashMap<>();
            toRecipient.put("email", toEmail);
            requestBody.put("to", java.util.Arrays.asList(toRecipient));

            // Subject và content
            requestBody.put("subject", subject);
            
            // Brevo API yêu cầu htmlContent là string trực tiếp, không phải object
            // Đảm bảo body không null hoặc empty
            if (body == null || body.trim().isEmpty()) {
                throw new IllegalArgumentException("Email body cannot be null or empty");
            }
            requestBody.put("htmlContent", body);

            // Tạo HTTP entity
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Gửi request đến Brevo API
            if (brevoApiUrl == null || brevoApiUrl.isEmpty()) {
                throw new IllegalStateException("Brevo API URL is not configured");
            }
            
            // Log request body để debug (ẩn api key)
            log.debug("Sending email to Brevo API. To: {}, Subject: {}, Body length: {}", 
                    toEmail, subject, body != null ? body.length() : 0);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    brevoApiUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email sent successfully to: {}", toEmail);
            } else {
                log.warn("⚠️ Email sent with status: {} to: {}", response.getStatusCode(), toEmail);
            }

        } catch (Exception e) {
            log.error("❌ Error sending email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
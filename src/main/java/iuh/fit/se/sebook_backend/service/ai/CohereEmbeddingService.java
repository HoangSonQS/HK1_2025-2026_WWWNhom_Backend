package iuh.fit.se.sebook_backend.service.ai;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CohereEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(CohereEmbeddingService.class);

    @Value("${cohere.api.key}")
    private String cohereApiKey;

    private static final String API_URL = "https://api.cohere.ai/v1/embed";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void checkEndpoint() {
        log.info("✅ Using Cohere endpoint: {}", API_URL);
    }

    public List<Double> generateEmbedding(String text) {
        int maxRetries = 3;
        int retryDelayMs = 3000; // Tăng delay lên 3 giây

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String textPreview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
                log.debug("🔄 Gọi Cohere API lần {} cho text: \"{}\"", attempt, textPreview);

                // Header
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + cohereApiKey);
                headers.set("Cohere-Version", "2022-12-06");
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Body
                JSONObject body = new JSONObject();
                body.put("model", "embed-multilingual-v3.0");
                body.put("input_type", "search_document");
                body.put("texts", new JSONArray().put(text));

                HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

                // Gửi request
                ResponseEntity<String> response = restTemplate.exchange(
                        API_URL, HttpMethod.POST, request, String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    JSONObject json = new JSONObject(response.getBody());
                    JSONArray embeddingsArray = json.getJSONArray("embeddings").getJSONArray(0);

                    List<Double> embedding = new ArrayList<>();
                    for (int i = 0; i < embeddingsArray.length(); i++) {
                        embedding.add(embeddingsArray.getDouble(i));
                    }

                    log.info("✅ Embedding sinh thành công ({} chiều)", embedding.size());
                    return embedding;

                } else {
                    log.warn("⚠️ Lỗi API: {} - {}", response.getStatusCode(), response.getBody());
                    // Nếu là rate limit (429), tăng delay trước khi retry
                    if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        log.warn("🚫 Rate limit detected! Sẽ đợi lâu hơn...");
                        retryDelayMs = 10000; // Đợi 10 giây nếu bị rate limit
                    }
                }

            } catch (Exception e) {
                log.error("❌ Lỗi khi gọi Cohere API (lần {}): {}", attempt, e.getMessage(), e);
            }

            if (attempt < maxRetries) {
                try {
                    log.debug("⏳ Đợi {}ms trước khi thử lại...", retryDelayMs);
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ignored) {}
            }
        }

        log.error("🚫 Không thể sinh embedding sau {} lần thử", maxRetries);
        return List.of();
    }
}


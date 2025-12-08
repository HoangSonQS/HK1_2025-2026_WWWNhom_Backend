package iuh.fit.se.sebook_backend.configs;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public String cohereApiKey(Dotenv dotenv) {
        // ✅ Ưu tiên lấy từ .env, nếu không có thì lấy từ environment variables
        String envKey = System.getenv("COHERE_API_KEY");
        String key = dotenv.get("COHERE_API_KEY", envKey != null ? envKey : "");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("❌ COHERE_API_KEY not found in .env or environment variables");
        }
        return key;
    }

    @Bean
    public String cohereEmbeddingModel(Dotenv dotenv) {
        // Model đa ngôn ngữ mạnh nhất hiện tại
        String envModel = System.getenv("COHERE_EMBEDDING_MODEL");
        String defaultModel = "embed-multilingual-v3.0";
        return dotenv.get("COHERE_EMBEDDING_MODEL", 
                envModel != null ? envModel : defaultModel);
    }
}


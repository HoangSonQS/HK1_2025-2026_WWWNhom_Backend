package iuh.fit.se.sebook_backend.service.ai;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AIService {

    private final CohereEmbeddingService cohereEmbeddingService;

    public AIService(CohereEmbeddingService cohereEmbeddingService) {
        this.cohereEmbeddingService = cohereEmbeddingService;
    }

    public List<Double> generateEmbedding(String text) {
        return cohereEmbeddingService.generateEmbedding(text);
    }
}


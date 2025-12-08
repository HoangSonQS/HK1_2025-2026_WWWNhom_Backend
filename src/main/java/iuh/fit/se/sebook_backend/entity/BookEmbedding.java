package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Entity
@Table(name = "book_embeddings")
@Getter
@Setter
public class BookEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    private Book book;

    @Column(name = "embedding_vector", columnDefinition = "TEXT")
    @Convert(converter = EmbeddingVectorConverter.class)
    private List<Double> embeddingVector;

    @Converter
    public static class EmbeddingVectorConverter implements AttributeConverter<List<Double>, String> {
        @Override
        public String convertToDatabaseColumn(List<Double> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            return attribute.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        }

        @Override
        public List<Double> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isEmpty()) {
                return new ArrayList<>();
            }
            List<Double> result = new ArrayList<>();
            String[] parts = dbData.split(",");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    result.add(Double.parseDouble(part.trim()));
                }
            }
            return result;
        }
    }
}


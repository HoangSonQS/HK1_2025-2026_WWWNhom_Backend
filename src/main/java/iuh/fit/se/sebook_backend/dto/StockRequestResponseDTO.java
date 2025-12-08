package iuh.fit.se.sebook_backend.dto;

import iuh.fit.se.sebook_backend.entity.StockRequestStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StockRequestResponseDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private int quantity;
    private String note;
    private StockRequestStatus status;
    private String createdByName;
    private String processedByName;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String responseNote;
}


package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PromotionLogResponseDTO {
    private Long promotionId;
    private String promotionCode;
    private String actorName; // Tên người thực hiện
    private String action; // Hành động (CREATE, APPROVE, DEACTIVATE)
    private LocalDateTime logTime;
}
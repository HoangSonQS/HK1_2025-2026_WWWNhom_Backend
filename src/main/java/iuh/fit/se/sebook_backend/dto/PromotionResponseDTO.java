package iuh.fit.se.sebook_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class PromotionResponseDTO {
    private Long id;
    private String name;
    private String code;
    private double discountPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private int quantity;

    @JsonProperty("priceOrderActive")
    private Double priceOrderActive; // Giá trị đơn hàng tối thiểu để áp dụng mã

    @JsonProperty("isActive")
    private boolean isActive;
    private String status;
    private String createdByName;
    private String approvedByName;
}
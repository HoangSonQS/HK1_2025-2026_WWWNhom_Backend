package iuh.fit.se.sebook_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class PromotionRequestDTO {
    private String name;
    private String code;
    private double discountPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private int quantity;

    @JsonProperty("priceOrderActive")
    private Double priceOrderActive; // Giá trị đơn hàng tối thiểu để áp dụng mã

    private String status; // Optional for admin updates
}
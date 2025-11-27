package iuh.fit.se.sebook_backend.dto;

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
    private Double priceOrderActive; // Giá trị đơn hàng tối thiểu để áp dụng mã
    private boolean isActive;
    private String createdByName;
    private String approvedByName;
}
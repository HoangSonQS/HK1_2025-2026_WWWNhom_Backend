package iuh.fit.se.sebook_backend.dto;

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
}
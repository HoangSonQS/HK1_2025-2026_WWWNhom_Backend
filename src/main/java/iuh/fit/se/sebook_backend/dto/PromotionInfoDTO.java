package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PromotionInfoDTO {
    private String code;
    private String name;
    private double discountPercent;
}


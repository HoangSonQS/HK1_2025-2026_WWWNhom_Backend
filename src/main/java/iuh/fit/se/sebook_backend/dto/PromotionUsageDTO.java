package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PromotionUsageDTO {
    private String code;
    private String name;
    private long totalOrders;
    private double totalRevenue; // totalAmount sau giảm
    private double totalDiscount; // tổng tiền giảm
}


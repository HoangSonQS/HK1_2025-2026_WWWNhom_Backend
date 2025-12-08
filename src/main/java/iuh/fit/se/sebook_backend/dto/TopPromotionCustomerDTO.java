package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TopPromotionCustomerDTO {
    private Long customerId;
    private String username;
    private String email;
    private long totalOrders;
    private double totalRevenue;
}


package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    private Long addressId;
    private String promotionCode;
    private String paymentMethod; // CASH, VNPAY, etc.
}
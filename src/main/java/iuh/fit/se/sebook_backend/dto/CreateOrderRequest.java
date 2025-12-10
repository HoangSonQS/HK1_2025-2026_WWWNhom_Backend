package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequest {
    private Long addressId;
    private String promotionCode;
    private String paymentMethod; // CASH, VNPAY, etc.
    private List<Long> cartItemIds; // Các cartItem được chọn để đặt hàng
}
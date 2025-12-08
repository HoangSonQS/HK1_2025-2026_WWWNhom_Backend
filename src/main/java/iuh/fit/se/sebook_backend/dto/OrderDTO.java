package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderDTO {
    private Long id;
    private LocalDateTime orderDate;
    private double totalAmount;
    private double subtotal; // Tổng tiền trước giảm giá
    private double discountAmount; // Số tiền giảm giá
    private String status;
    private String paymentMethod; // CASH, VNPAY, etc.
    private AddressDTO deliveryAddress;
    private PromotionInfoDTO appliedPromotion; // Thông tin khuyến mãi đã áp dụng
    private CustomerInfoDTO customerInfo; // Thông tin khách hàng
    private List<OrderDetailDTO> orderDetails;
}
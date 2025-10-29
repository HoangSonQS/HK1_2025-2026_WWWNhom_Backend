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
    private String status;
    private List<OrderDetailDTO> orderDetails;
}
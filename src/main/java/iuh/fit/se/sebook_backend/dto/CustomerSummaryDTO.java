package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CustomerSummaryDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private long totalOrders;
    private double totalSpending; // chỉ tính đơn COMPLETED
    private LocalDateTime lastOrderDate;
}


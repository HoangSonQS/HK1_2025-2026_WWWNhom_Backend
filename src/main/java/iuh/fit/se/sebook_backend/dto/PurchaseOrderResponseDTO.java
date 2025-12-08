package iuh.fit.se.sebook_backend.dto;

import iuh.fit.se.sebook_backend.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class PurchaseOrderResponseDTO {
    private Long id;
    private String supplierName;
    private PurchaseOrderStatus status;
    private String createdByName;
    private String approvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String note;
    private List<PurchaseOrderItemDTO> items;
}


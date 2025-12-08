package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequestDTO {
    private Long supplierId;
    private String note;
    private List<PurchaseOrderItemDTO> items;
}


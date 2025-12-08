package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InventoryCategoryDTO {
    private Long categoryId;
    private String categoryName;
    private long totalQuantity;
    private double totalValue;
}


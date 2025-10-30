package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportStockItemRequestDTO {
    private Long bookId;
    private int quantity;
    private double importPrice;
}
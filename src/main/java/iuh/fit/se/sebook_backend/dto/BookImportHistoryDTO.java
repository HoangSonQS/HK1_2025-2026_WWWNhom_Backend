package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BookImportHistoryDTO {
    private Long importStockId;
    private String supplierName;
    private String createdByName;
    private LocalDateTime importDate;
    private int quantity;
    private double importPrice;
    private double totalAmount; // quantity * importPrice
}

package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ImportStockResponseDTO {
    private Long id;
    private String supplierName;
    private String createdByName;
    private LocalDateTime importDate;
    private List<ImportStockDetailResponseDTO> items;
}
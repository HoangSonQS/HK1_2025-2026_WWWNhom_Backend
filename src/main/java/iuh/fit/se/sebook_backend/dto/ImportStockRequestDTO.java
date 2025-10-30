package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ImportStockRequestDTO {
    private Long supplierId;
    private List<ImportStockItemRequestDTO> items;
}
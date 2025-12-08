package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockRequestApproveDTO {
    private Long supplierId;
    private double importPrice;
    private String responseNote;
}


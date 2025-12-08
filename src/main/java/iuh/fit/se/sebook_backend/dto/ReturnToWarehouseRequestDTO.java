package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnToWarehouseRequestDTO {
    private Long bookId;
    private int quantity;
    private String note;
}


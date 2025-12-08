package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StockCheckResultDTO {
    private Long bookId;
    private String title;
    private int systemQuantity;
    private int countedQuantity;
    private int difference;
}


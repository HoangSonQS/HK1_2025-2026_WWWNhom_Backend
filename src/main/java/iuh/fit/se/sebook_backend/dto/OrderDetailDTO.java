package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderDetailDTO {
    private Long bookId;
    private String bookTitle;
    private int quantity;
    private double priceAtPurchase;
}
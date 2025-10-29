package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CartItemDTO {
    private Long cartItemId;
    private Long bookId;
    private String bookTitle;
    private String bookImageUrl;
    private double bookPrice;
    private int quantity;
}
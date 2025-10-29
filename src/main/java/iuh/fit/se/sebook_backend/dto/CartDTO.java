package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class CartDTO {
    private List<CartItemDTO> items;
    private double totalPrice;
}
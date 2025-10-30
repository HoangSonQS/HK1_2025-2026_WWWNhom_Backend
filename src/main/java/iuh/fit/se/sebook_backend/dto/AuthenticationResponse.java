package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String refreshToken;

    @Getter
    @Setter
    @Builder
    public static class CartDTO {
        private List<CartItemDTO> items;
        private double totalPrice;
    }
}
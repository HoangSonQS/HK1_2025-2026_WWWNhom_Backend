package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentReturnDTO {
    private boolean success;
    private Long orderId;
    private String message;
}


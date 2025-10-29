package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {
    private String status; // Trạng thái mới (PENDING, PROCESSING, etc.)
}
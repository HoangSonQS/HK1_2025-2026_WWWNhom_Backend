package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountStatusUpdateRequest {
    private boolean isActive;
}
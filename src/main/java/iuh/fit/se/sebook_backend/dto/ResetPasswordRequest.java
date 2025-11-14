package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String username;
    private String token;
    private String newPassword;
}


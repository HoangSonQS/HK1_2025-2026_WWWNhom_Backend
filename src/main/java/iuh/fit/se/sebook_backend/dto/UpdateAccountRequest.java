package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountRequest {
    private String username;
    private String email;
}


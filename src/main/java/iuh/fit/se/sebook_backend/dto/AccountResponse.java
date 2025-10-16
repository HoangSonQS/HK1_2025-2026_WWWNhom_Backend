package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private boolean isActive;
    private Set<String> roles;
}
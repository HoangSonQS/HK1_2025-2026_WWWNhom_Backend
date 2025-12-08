package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateAccountRolesRequest {
    private Set<String> roles;
}


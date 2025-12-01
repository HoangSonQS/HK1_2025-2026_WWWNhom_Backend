package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateStaffAccountRequest {
    private String username;
    private String password;
    private String email;
    private Set<String> roles; // ADMIN, SELLER_STAFF, WAREHOUSE_STAFF
}


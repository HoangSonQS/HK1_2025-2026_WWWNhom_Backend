package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private boolean isActive;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Set<String> roles;
    private List<AddressDTO> addresses;
}
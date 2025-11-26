package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CustomerInfoDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}


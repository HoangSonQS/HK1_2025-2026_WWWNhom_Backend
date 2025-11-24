package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddressDTO {
    private Long id;
    private String addressType;
    private boolean isDefault;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String phoneNumber;
    private String recipientName;
}


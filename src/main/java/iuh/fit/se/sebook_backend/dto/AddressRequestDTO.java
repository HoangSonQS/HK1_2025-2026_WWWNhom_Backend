package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequestDTO {
    private String addressType; // HOME, OFFICE, OTHER, etc.
    private Boolean isDefault; // null nếu không muốn thay đổi
    private String street;
    private String ward;
    private String district;
    private String city;
    private String phoneNumber;
    private String recipientName;
}


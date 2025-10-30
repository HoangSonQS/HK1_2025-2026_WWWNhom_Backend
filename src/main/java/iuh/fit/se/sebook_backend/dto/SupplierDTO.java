package iuh.fit.se.sebook_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private boolean isActive;
}
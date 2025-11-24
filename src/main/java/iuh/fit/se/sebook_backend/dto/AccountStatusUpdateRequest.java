package iuh.fit.se.sebook_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountStatusUpdateRequest {
    @JsonProperty("isActive")
    private Boolean isActive;
}
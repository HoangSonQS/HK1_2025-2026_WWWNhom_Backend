package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CustomerDetailDTO {
    private CustomerSummaryDTO summary;
    private List<OrderDTO> orders;
}


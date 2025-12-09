package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnReportDTO {
    private long count;
    private double totalAmount;
}


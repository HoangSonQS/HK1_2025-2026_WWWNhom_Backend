package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class RevenuePointDTO {
    private LocalDate date;
    private double revenue;
    private long orders;
}


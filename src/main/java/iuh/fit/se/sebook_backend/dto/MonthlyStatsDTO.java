package iuh.fit.se.sebook_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatsDTO {
    private int year;
    private int month; // 1-12
    private double revenue;
    private long orders;
}


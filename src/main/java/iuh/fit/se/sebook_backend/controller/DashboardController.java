package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.DashboardSummaryDTO;
import iuh.fit.se.sebook_backend.dto.MonthlyStatsDTO;
import iuh.fit.se.sebook_backend.dto.TopSellingProductDTO;
import iuh.fit.se.sebook_backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlyStatsDTO>> getMonthlyStats(Integer months, Integer year) {
        int limit = (months == null) ? 12 : months;
        return ResponseEntity.ok(dashboardService.getMonthlyStats(limit, year));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopSellingProductDTO>> getTopSellingProducts() {
        return ResponseEntity.ok(dashboardService.getTopSellingProducts());
    }
}
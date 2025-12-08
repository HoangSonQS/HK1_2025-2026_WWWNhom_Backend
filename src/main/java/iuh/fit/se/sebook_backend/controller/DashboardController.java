package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.DashboardSummaryDTO;
import iuh.fit.se.sebook_backend.dto.LowStockDTO;
import iuh.fit.se.sebook_backend.dto.RevenuePointDTO;
import iuh.fit.se.sebook_backend.dto.StatusCountDTO;
import iuh.fit.se.sebook_backend.dto.PromotionUsageDTO;
import iuh.fit.se.sebook_backend.dto.TopPromotionCustomerDTO;
import iuh.fit.se.sebook_backend.dto.TopSellingProductDTO;
import iuh.fit.se.sebook_backend.dto.InventorySummaryDTO;
import iuh.fit.se.sebook_backend.dto.InventoryCategoryDTO;
import iuh.fit.se.sebook_backend.dto.WarehouseSummaryDTO;
import iuh.fit.se.sebook_backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.time.LocalDate;

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

    @GetMapping("/top-products")
    public ResponseEntity<List<TopSellingProductDTO>> getTopSellingProducts() {
        return ResponseEntity.ok(dashboardService.getTopSellingProducts());
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenuePointDTO>> getRevenueByRange(
            @RequestParam("start") String start,
            @RequestParam("end") String endExclusive) {
        return ResponseEntity.ok(dashboardService.getRevenueByDateRange(
                LocalDate.parse(start), LocalDate.parse(endExclusive)));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<List<StatusCountDTO>> getStatusCounts(
            @RequestParam("start") String start,
            @RequestParam("end") String endExclusive) {
        return ResponseEntity.ok(dashboardService.getStatusCounts(
                LocalDate.parse(start), LocalDate.parse(endExclusive)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockDTO>> getLowStock(
            @RequestParam(value = "threshold", defaultValue = "10") int threshold) {
        return ResponseEntity.ok(dashboardService.getLowStock(threshold));
    }

    @GetMapping("/promotion-usage")
    public ResponseEntity<List<PromotionUsageDTO>> getPromotionUsage() {
        return ResponseEntity.ok(dashboardService.getPromotionUsage());
    }

    @GetMapping("/promotion-top-customers")
    public ResponseEntity<List<TopPromotionCustomerDTO>> getTopPromotionCustomers() {
        return ResponseEntity.ok(dashboardService.getTopPromotionCustomers());
    }

    @GetMapping("/inventory/summary")
    public ResponseEntity<InventorySummaryDTO> getInventorySummary() {
        return ResponseEntity.ok(dashboardService.getInventorySummary());
    }

    @GetMapping("/inventory/categories")
    public ResponseEntity<List<InventoryCategoryDTO>> getInventoryByCategory() {
        return ResponseEntity.ok(dashboardService.getInventoryByCategory());
    }

    @GetMapping("/warehouse-summary")
    public ResponseEntity<WarehouseSummaryDTO> getWarehouseSummary() {
        return ResponseEntity.ok(dashboardService.getWarehouseSummary());
    }
}
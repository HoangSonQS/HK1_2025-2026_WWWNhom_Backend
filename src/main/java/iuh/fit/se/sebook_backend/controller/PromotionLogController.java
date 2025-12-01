package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.PromotionLogResponseDTO;
import iuh.fit.se.sebook_backend.dto.PromotionResponseDTO;
import iuh.fit.se.sebook_backend.service.PromotionLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/promotion-logs")
public class PromotionLogController {
    private final PromotionLogService promotionLogService;

    public PromotionLogController(PromotionLogService promotionLogService) {
        this.promotionLogService = promotionLogService;
    }

    @GetMapping("/action")
    public ResponseEntity<List<PromotionLogResponseDTO>> getLogsByAction(@RequestParam String action) {
        return ResponseEntity.ok(promotionLogService.getLogsByAction(action));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PromotionLogResponseDTO>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(promotionLogService.getLogsByDateRange(startDate, endDate));
    }

    @GetMapping("/promotions-by-date-range")
    public ResponseEntity<List<PromotionResponseDTO>> getPromotionsByLogDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(promotionLogService.getPromotionsByLogTimeRange(startDate, endDate));
    }
}
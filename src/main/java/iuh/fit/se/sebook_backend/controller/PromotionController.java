package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.PromotionRequestDTO;
import iuh.fit.se.sebook_backend.dto.PromotionResponseDTO;
import iuh.fit.se.sebook_backend.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    public ResponseEntity<PromotionResponseDTO> createPromotion(@RequestBody PromotionRequestDTO request) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PromotionResponseDTO>> searchPromotions(@RequestParam String keyword) {
        return ResponseEntity.ok(promotionService.searchPromotions(keyword));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PromotionResponseDTO> approvePromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.approvePromotion(id));
    }

    @DeleteMapping("/{id}") // Xóa mềm
    public ResponseEntity<PromotionResponseDTO> deactivatePromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.deactivatePromotion(id));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<PromotionResponseDTO> pausePromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.pausePromotion(id));
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<PromotionResponseDTO> resumePromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.resumePromotion(id));
    }

    @GetMapping("/validate")
    public ResponseEntity<PromotionResponseDTO> validatePromotionCode(@RequestParam String code) {
        return ResponseEntity.ok(promotionService.validatePromotionCode(code));
    }
}
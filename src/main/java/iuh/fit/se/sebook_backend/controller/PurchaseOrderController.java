package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.PurchaseOrderRequestDTO;
import iuh.fit.se.sebook_backend.dto.PurchaseOrderResponseDTO;
import iuh.fit.se.sebook_backend.service.PurchaseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    public PurchaseOrderController(PurchaseOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDTO> create(@RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrderResponseDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PurchaseOrderResponseDTO> reject(@PathVariable Long id, @RequestBody(required = false) String note) {
        return ResponseEntity.ok(service.reject(id, note));
    }
}


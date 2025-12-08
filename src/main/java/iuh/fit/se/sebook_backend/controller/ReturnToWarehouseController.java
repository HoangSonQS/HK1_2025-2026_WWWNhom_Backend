package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseProcessDTO;
import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseRequestDTO;
import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseResponseDTO;
import iuh.fit.se.sebook_backend.service.ReturnToWarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse-returns")
public class ReturnToWarehouseController {

    private final ReturnToWarehouseService service;

    public ReturnToWarehouseController(ReturnToWarehouseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReturnToWarehouseResponseDTO> create(@RequestBody ReturnToWarehouseRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnToWarehouseResponseDTO>> myRequests() {
        return ResponseEntity.ok(service.getMyRequests());
    }

    @GetMapping
    public ResponseEntity<List<ReturnToWarehouseResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ReturnToWarehouseResponseDTO> approve(@PathVariable Long id,
                                                                @RequestBody ReturnToWarehouseProcessDTO dto) {
        return ResponseEntity.ok(service.approve(id, dto));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ReturnToWarehouseResponseDTO> reject(@PathVariable Long id,
                                                               @RequestBody ReturnToWarehouseProcessDTO dto) {
        return ResponseEntity.ok(service.reject(id, dto));
    }
}


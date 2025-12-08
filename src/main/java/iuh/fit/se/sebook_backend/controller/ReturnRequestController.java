package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.ReturnRequestCreateDTO;
import iuh.fit.se.sebook_backend.dto.ReturnRequestProcessDTO;
import iuh.fit.se.sebook_backend.dto.ReturnRequestResponseDTO;
import iuh.fit.se.sebook_backend.service.ReturnRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/return-requests")
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @PostMapping
    public ResponseEntity<ReturnRequestResponseDTO> create(@RequestBody ReturnRequestCreateDTO dto) {
        return ResponseEntity.ok(returnRequestService.create(dto));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnRequestResponseDTO>> getMy() {
        return ResponseEntity.ok(returnRequestService.getMyRequests());
    }

    @GetMapping
    public ResponseEntity<List<ReturnRequestResponseDTO>> getAll() {
        return ResponseEntity.ok(returnRequestService.getAllRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ReturnRequestResponseDTO> approve(@PathVariable Long id,
                                                            @RequestBody ReturnRequestProcessDTO dto) {
        return ResponseEntity.ok(returnRequestService.approve(id, dto));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ReturnRequestResponseDTO> reject(@PathVariable Long id,
                                                           @RequestBody ReturnRequestProcessDTO dto) {
        return ResponseEntity.ok(returnRequestService.reject(id, dto));
    }
}


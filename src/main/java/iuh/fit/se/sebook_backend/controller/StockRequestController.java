package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.StockRequestApproveDTO;
import iuh.fit.se.sebook_backend.dto.StockRequestRejectDTO;
import iuh.fit.se.sebook_backend.dto.StockRequestRequestDTO;
import iuh.fit.se.sebook_backend.dto.StockRequestResponseDTO;
import iuh.fit.se.sebook_backend.service.StockRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-requests")
public class StockRequestController {

    private final StockRequestService stockRequestService;

    public StockRequestController(StockRequestService stockRequestService) {
        this.stockRequestService = stockRequestService;
    }

    @PostMapping
    public ResponseEntity<StockRequestResponseDTO> createRequest(@RequestBody StockRequestRequestDTO request) {
        return ResponseEntity.ok(stockRequestService.createRequest(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<StockRequestResponseDTO>> getMyRequests() {
        return ResponseEntity.ok(stockRequestService.getMyRequests());
    }

    @GetMapping
    public ResponseEntity<List<StockRequestResponseDTO>> getAllRequests() {
        return ResponseEntity.ok(stockRequestService.getAllRequests());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<StockRequestResponseDTO> approve(@PathVariable Long id,
                                                           @RequestBody StockRequestApproveDTO approveDTO) {
        return ResponseEntity.ok(stockRequestService.approve(id, approveDTO));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<StockRequestResponseDTO> reject(@PathVariable Long id,
                                                          @RequestBody StockRequestRejectDTO rejectDTO) {
        return ResponseEntity.ok(stockRequestService.reject(id, rejectDTO.getResponseNote()));
    }
}


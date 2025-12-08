package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.BookImportHistoryDTO;
import iuh.fit.se.sebook_backend.dto.ImportStockRequestDTO;
import iuh.fit.se.sebook_backend.dto.ImportStockResponseDTO;
import iuh.fit.se.sebook_backend.service.ImportStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/import-stocks")
public class ImportStockController {
    private final ImportStockService importStockService;

    public ImportStockController(ImportStockService importStockService) {
        this.importStockService = importStockService;
    }

    @PostMapping
    public ResponseEntity<ImportStockResponseDTO> createImportStock(@RequestBody ImportStockRequestDTO request) {
        return ResponseEntity.ok(importStockService.createImportStock(request));
    }

    @GetMapping
    public ResponseEntity<List<ImportStockResponseDTO>> getAllImportStocks() {
        return ResponseEntity.ok(importStockService.getAllImportStocks());
    }

    @GetMapping("/books/{bookId}/history")
    public ResponseEntity<List<BookImportHistoryDTO>> getImportHistoryByBookId(@PathVariable Long bookId) {
        return ResponseEntity.ok(importStockService.getImportHistoryByBookId(bookId));
    }
}
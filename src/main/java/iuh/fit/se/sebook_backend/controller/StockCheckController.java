package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.StockCheckResultDTO;
import iuh.fit.se.sebook_backend.service.StockCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/stock-checks")
public class StockCheckController {

    private final StockCheckService stockCheckService;

    public StockCheckController(StockCheckService stockCheckService) {
        this.stockCheckService = stockCheckService;
    }

    @PostMapping("/compare")
    public ResponseEntity<List<StockCheckResultDTO>> compare(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(stockCheckService.compare(file));
    }
}


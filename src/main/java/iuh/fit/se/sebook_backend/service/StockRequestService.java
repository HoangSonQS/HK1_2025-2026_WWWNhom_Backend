package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.StockRequestRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockRequestService {

    private final StockRequestRepository stockRequestRepository;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;
    private final ImportStockService importStockService;

    public StockRequestService(StockRequestRepository stockRequestRepository,
                               BookRepository bookRepository,
                               SecurityUtil securityUtil,
                               ImportStockService importStockService) {
        this.stockRequestRepository = stockRequestRepository;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
        this.importStockService = importStockService;
    }

    @Transactional
    public StockRequestResponseDTO createRequest(StockRequestRequestDTO request) {
        Account current = securityUtil.getLoggedInAccount();

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        StockRequest stockRequest = new StockRequest();
        stockRequest.setBook(book);
        stockRequest.setQuantity(request.getQuantity());
        stockRequest.setNote(request.getNote());
        stockRequest.setStatus(StockRequestStatus.PENDING);
        stockRequest.setCreatedBy(current);
        stockRequest.setCreatedAt(LocalDateTime.now());

        StockRequest saved = stockRequestRepository.save(stockRequest);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StockRequestResponseDTO> getMyRequests() {
        Account current = securityUtil.getLoggedInAccount();
        return stockRequestRepository.findByCreatedByIdOrderByCreatedAtDesc(current.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockRequestResponseDTO> getAllRequests() {
        return stockRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public StockRequestResponseDTO approve(Long id, StockRequestApproveDTO approveDTO) {
        Account current = securityUtil.getLoggedInAccount();
        StockRequest stockRequest = stockRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock request not found"));

        if (stockRequest.getStatus() != StockRequestStatus.PENDING) {
            throw new IllegalStateException("Stock request already processed");
        }

        // Tạo phiếu nhập kho dựa trên yêu cầu
        ImportStockRequestDTO importRequest = new ImportStockRequestDTO();
        importRequest.setSupplierId(approveDTO.getSupplierId());
        ImportStockItemRequestDTO itemRequest = new ImportStockItemRequestDTO();
        itemRequest.setBookId(stockRequest.getBook().getId());
        itemRequest.setQuantity(stockRequest.getQuantity());
        itemRequest.setImportPrice(approveDTO.getImportPrice());
        importRequest.setItems(List.of(itemRequest));

        importStockService.createImportStock(importRequest);

        stockRequest.setStatus(StockRequestStatus.APPROVED);
        stockRequest.setProcessedBy(current);
        stockRequest.setProcessedAt(LocalDateTime.now());
        stockRequest.setResponseNote(approveDTO.getResponseNote());

        StockRequest saved = stockRequestRepository.save(stockRequest);
        return toDto(saved);
    }

    @Transactional
    public StockRequestResponseDTO reject(Long id, String responseNote) {
        Account current = securityUtil.getLoggedInAccount();
        StockRequest stockRequest = stockRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock request not found"));

        if (stockRequest.getStatus() != StockRequestStatus.PENDING) {
            throw new IllegalStateException("Stock request already processed");
        }

        stockRequest.setStatus(StockRequestStatus.REJECTED);
        stockRequest.setProcessedBy(current);
        stockRequest.setProcessedAt(LocalDateTime.now());
        stockRequest.setResponseNote(responseNote);

        StockRequest saved = stockRequestRepository.save(stockRequest);
        return toDto(saved);
    }

    private StockRequestResponseDTO toDto(StockRequest request) {
        return StockRequestResponseDTO.builder()
                .id(request.getId())
                .bookId(request.getBook().getId())
                .bookTitle(request.getBook().getTitle())
                .quantity(request.getQuantity())
                .note(request.getNote())
                .status(request.getStatus())
                .createdByName(request.getCreatedBy() != null ? request.getCreatedBy().getUsername() : null)
                .processedByName(request.getProcessedBy() != null ? request.getProcessedBy().getUsername() : null)
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .responseNote(request.getResponseNote())
                .build();
    }
}


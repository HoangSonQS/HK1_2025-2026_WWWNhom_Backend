package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.*;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImportStockService {
    private final ImportStockRepository importStockRepository;
    private final SupplierRepository supplierRepository;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;

    public ImportStockService(ImportStockRepository importStockRepository, SupplierRepository supplierRepository,
                              BookRepository bookRepository, SecurityUtil securityUtil) {
        this.importStockRepository = importStockRepository;
        this.supplierRepository = supplierRepository;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
    }

    /**
     * Tạo một phiếu nhập kho mới.
     * Cập nhật số lượng tồn kho của sách.
     */
    @Transactional
    public ImportStockResponseDTO createImportStock(ImportStockRequestDTO request) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        ImportStock importStock = new ImportStock();
        importStock.setSupplier(supplier);
        importStock.setCreatedBy(currentUser);
        importStock.setImportDate(LocalDateTime.now());

        List<ImportStockDetail> details = new ArrayList<>();

        for (ImportStockItemRequestDTO itemDTO : request.getItems()) {
            Book book = bookRepository.findById(itemDTO.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found: " + itemDTO.getBookId()));

            // Cập nhật số lượng tồn kho của sách
            book.setQuantity(book.getQuantity() + itemDTO.getQuantity());
            bookRepository.save(book);

            // Tạo chi tiết phiếu nhập
            ImportStockDetail detail = new ImportStockDetail();
            detail.setImportStock(importStock);
            detail.setBook(book);
            detail.setQuantity(itemDTO.getQuantity());
            detail.setImportPrice(itemDTO.getImportPrice());
            details.add(detail);
        }

        importStock.setImportStockDetails(details);
        ImportStock savedImport = importStockRepository.save(importStock);

        return toResponseDto(savedImport);
    }

    /**
     * Lấy tất cả các phiếu nhập kho.
     */
    @Transactional(readOnly = true)
    public List<ImportStockResponseDTO> getAllImportStocks() {
        return importStockRepository.findAll().stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    private ImportStockResponseDTO toResponseDto(ImportStock importStock) {
        List<ImportStockDetailResponseDTO> itemDTOs = importStock.getImportStockDetails().stream()
                .map(detail -> ImportStockDetailResponseDTO.builder()
                        .bookId(detail.getBook().getId())
                        .bookTitle(detail.getBook().getTitle())
                        .quantity(detail.getQuantity())
                        .importPrice(detail.getImportPrice())
                        .build())
                .collect(Collectors.toList());

        return ImportStockResponseDTO.builder()
                .id(importStock.getId())
                .supplierName(importStock.getSupplier().getName())
                .createdByName(importStock.getCreatedBy().getUsername())
                .importDate(importStock.getImportDate())
                .items(itemDTOs)
                .build();
    }
}
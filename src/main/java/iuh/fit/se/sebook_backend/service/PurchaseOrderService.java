package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.*;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.PurchaseOrderRepository;
import iuh.fit.se.sebook_backend.repository.SupplierRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;
    private final ImportStockService importStockService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                BookRepository bookRepository,
                                SecurityUtil securityUtil,
                                ImportStockService importStockService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
        this.importStockService = importStockService;
    }

    @Transactional
    public PurchaseOrderResponseDTO create(PurchaseOrderRequestDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        Long supplierId = dto.getSupplierId();
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier id is required");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setCreatedBy(current);
        po.setCreatedAt(LocalDateTime.now());
        po.setNote(dto.getNote());

        List<PurchaseOrderItem> items = new ArrayList<>();
        for (PurchaseOrderItemDTO itemDto : dto.getItems()) {
            Long bookId = itemDto.getBookId();
            if (bookId == null) {
                throw new IllegalArgumentException("Book id is required");
            }
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found: " + itemDto.getBookId()));
            PurchaseOrderItem it = new PurchaseOrderItem();
            it.setPurchaseOrder(po);
            it.setBook(book);
            it.setQuantity(itemDto.getQuantity());
            it.setImportPrice(itemDto.getImportPrice());
            items.add(it);
        }
        po.setItems(items);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponseDTO> getAll() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseOrderResponseDTO approve(Long id) {
        Account current = securityUtil.getLoggedInAccount();
        if (id == null) {
            throw new IllegalArgumentException("Purchase order id is null");
        }
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Purchase order already processed");
        }

        // Convert to import stock
        ImportStockRequestDTO importReq = new ImportStockRequestDTO();
        importReq.setSupplierId(po.getSupplier().getId());
        List<ImportStockItemRequestDTO> importItems = po.getItems().stream().map(it -> {
            ImportStockItemRequestDTO item = new ImportStockItemRequestDTO();
            item.setBookId(it.getBook().getId());
            item.setQuantity(it.getQuantity());
            item.setImportPrice(it.getImportPrice());
            return item;
        }).collect(Collectors.toList());
        importReq.setItems(importItems);
        importStockService.createImportStock(importReq);

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(current);
        po.setApprovedAt(LocalDateTime.now());
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return toDto(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO reject(Long id, String note) {
        Account current = securityUtil.getLoggedInAccount();
        if (id == null) {
            throw new IllegalArgumentException("Purchase order id is null");
        }
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found"));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Purchase order already processed");
        }

        po.setStatus(PurchaseOrderStatus.REJECTED);
        po.setApprovedBy(current);
        po.setApprovedAt(LocalDateTime.now());
        po.setNote(note);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return toDto(saved);
    }

    private PurchaseOrderResponseDTO toDto(PurchaseOrder po) {
        List<PurchaseOrderItemDTO> itemDTOs = po.getItems() == null ? List.of() :
                po.getItems().stream().map(it -> {
                    PurchaseOrderItemDTO dto = new PurchaseOrderItemDTO();
                    dto.setBookId(it.getBook().getId());
                    dto.setQuantity(it.getQuantity());
                    dto.setImportPrice(it.getImportPrice());
                    return dto;
                }).collect(Collectors.toList());

        return PurchaseOrderResponseDTO.builder()
                .id(po.getId())
                .supplierName(po.getSupplier().getName())
                .status(po.getStatus())
                .createdByName(po.getCreatedBy() != null ? po.getCreatedBy().getUsername() : null)
                .approvedByName(po.getApprovedBy() != null ? po.getApprovedBy().getUsername() : null)
                .createdAt(po.getCreatedAt())
                .approvedAt(po.getApprovedAt())
                .note(po.getNote())
                .items(itemDTOs)
                .build();
    }
}


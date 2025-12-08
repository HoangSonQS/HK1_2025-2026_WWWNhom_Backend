package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseProcessDTO;
import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseRequestDTO;
import iuh.fit.se.sebook_backend.dto.ReturnToWarehouseResponseDTO;
import iuh.fit.se.sebook_backend.entity.*;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.ReturnToWarehouseRequestRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReturnToWarehouseService {

    private final ReturnToWarehouseRequestRepository repo;
    private final BookRepository bookRepository;
    private final SecurityUtil securityUtil;

    public ReturnToWarehouseService(ReturnToWarehouseRequestRepository repo,
                                    BookRepository bookRepository,
                                    SecurityUtil securityUtil) {
        this.repo = repo;
        this.bookRepository = bookRepository;
        this.securityUtil = securityUtil;
    }

    @Transactional
    public ReturnToWarehouseResponseDTO create(ReturnToWarehouseRequestDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        ReturnToWarehouseRequest req = new ReturnToWarehouseRequest();
        req.setBook(book);
        req.setQuantity(dto.getQuantity());
        req.setNote(dto.getNote());
        req.setStatus(ReturnToWarehouseStatus.PENDING);
        req.setCreatedBy(current);
        req.setCreatedAt(LocalDateTime.now());

        ReturnToWarehouseRequest saved = repo.save(req);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReturnToWarehouseResponseDTO> getMyRequests() {
        Account current = securityUtil.getLoggedInAccount();
        return repo.findByCreatedByIdOrderByCreatedAtDesc(current.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnToWarehouseResponseDTO> getAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ReturnToWarehouseResponseDTO approve(Long id, ReturnToWarehouseProcessDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        ReturnToWarehouseRequest req = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return-to-warehouse request not found"));

        if (req.getStatus() != ReturnToWarehouseStatus.PENDING) {
            throw new IllegalStateException("Request already processed");
        }

        // Cộng tồn kho
        Book book = req.getBook();
        book.setQuantity(book.getQuantity() + req.getQuantity());
        bookRepository.save(book);

        req.setStatus(ReturnToWarehouseStatus.APPROVED);
        req.setProcessedBy(current);
        req.setProcessedAt(LocalDateTime.now());
        req.setResponseNote(dto.getResponseNote());

        ReturnToWarehouseRequest saved = repo.save(req);
        return toDto(saved);
    }

    @Transactional
    public ReturnToWarehouseResponseDTO reject(Long id, ReturnToWarehouseProcessDTO dto) {
        Account current = securityUtil.getLoggedInAccount();
        ReturnToWarehouseRequest req = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return-to-warehouse request not found"));

        if (req.getStatus() != ReturnToWarehouseStatus.PENDING) {
            throw new IllegalStateException("Request already processed");
        }

        req.setStatus(ReturnToWarehouseStatus.REJECTED);
        req.setProcessedBy(current);
        req.setProcessedAt(LocalDateTime.now());
        req.setResponseNote(dto.getResponseNote());

        ReturnToWarehouseRequest saved = repo.save(req);
        return toDto(saved);
    }

    private ReturnToWarehouseResponseDTO toDto(ReturnToWarehouseRequest req) {
        return ReturnToWarehouseResponseDTO.builder()
                .id(req.getId())
                .bookId(req.getBook().getId())
                .bookTitle(req.getBook().getTitle())
                .quantity(req.getQuantity())
                .note(req.getNote())
                .status(req.getStatus())
                .createdByName(req.getCreatedBy() != null ? req.getCreatedBy().getUsername() : null)
                .processedByName(req.getProcessedBy() != null ? req.getProcessedBy().getUsername() : null)
                .createdAt(req.getCreatedAt())
                .processedAt(req.getProcessedAt())
                .responseNote(req.getResponseNote())
                .build();
    }
}


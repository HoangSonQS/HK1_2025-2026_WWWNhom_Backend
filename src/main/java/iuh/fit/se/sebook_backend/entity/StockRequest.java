package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_requests")
@Getter
@Setter
public class StockRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockRequestStatus status = StockRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    private Account createdBy; // Seller staff

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_account_id")
    private Account processedBy; // Warehouse staff

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime processedAt;

    @Column(length = 500)
    private String responseNote;
}


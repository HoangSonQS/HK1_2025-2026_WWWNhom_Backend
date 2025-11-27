package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private double discountPercent; // Phần trăm giảm giá (ví dụ: 10.5 cho 10.5%)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = true)
    private Double priceOrderActive; // giá để có thể áp dụng mã

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Dùng cho xóa mềm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_account_id")
    private Account createdBy; // Nhân viên (staff) tạo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_account_id")
    private Account approvedBy; // Nhân viên (staff) duyệt

    // Liên kết với các đơn hàng đã áp dụng
    @OneToMany(mappedBy = "appliedPromotion")
    private List<Order> orders;
}
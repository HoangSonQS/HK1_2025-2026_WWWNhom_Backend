package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    public static final String PENDING = "PENDING"; // Chờ xử lý
    public static final String PROCESSING = "PROCESSING"; // Đang xử lý (đã xác nhận)
    public static final String DELIVERING = "DELIVERING"; // Đang giao
    public static final String COMPLETED = "COMPLETED"; // Đã giao thành công
    public static final String CANCELLED = "CANCELLED"; // Đã hủy
    public static final String RETURNED = "RETURNED"; // Đã trả lại

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(length = 50, nullable = false)
    private String status; // Ví dụ: PENDING, PROCESSING, COMPLETED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion appliedPromotion;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;
}
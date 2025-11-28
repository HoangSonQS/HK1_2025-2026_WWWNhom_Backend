package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_logs")
@Getter
@Setter
public class PromotionLog {

    public static final String CREATE = "CREATE";
    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";
    public static final String DEACTIVATE = "DEACTIVATE"; // Xóa mềm (legacy)
    public static final String PAUSE = "PAUSE";
    public static final String RESUME = "RESUME";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id", nullable = false)
    private Account actor; // Nhân viên (staff) thực hiện hành động

    @Column(length = 50, nullable = false)
    private String action; // CREATE, APPROVE, REJECT, DEACTIVATE

    @Column(name = "log_time", nullable = false)
    private LocalDateTime logTime;
}
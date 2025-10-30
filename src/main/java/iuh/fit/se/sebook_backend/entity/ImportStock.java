package iuh.fit.se.sebook_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "import_stocks")
@Getter
@Setter
public class ImportStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    private Account createdBy; // Tài khoản nhân viên kho tạo phiếu

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @OneToMany(mappedBy = "importStock", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImportStockDetail> importStockDetails;
}
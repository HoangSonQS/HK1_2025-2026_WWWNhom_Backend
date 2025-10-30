package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    // Hỗ trợ tìm kiếm khuyến mãi theo tên hoặc mã (theo kế hoạch)
    List<Promotion> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);

    // Tìm khuyến mãi theo mã (để kiểm tra trùng lặp)
    Optional<Promotion> findByCode(String code);

    // Tìm khuyến mãi còn hoạt động và còn hạn sử dụng
    Optional<Promotion> findByCodeAndIsActiveTrueAndEndDateAfter(String code, LocalDate today);
}
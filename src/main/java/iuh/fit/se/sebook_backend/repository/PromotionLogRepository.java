package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.PromotionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionLogRepository extends JpaRepository<PromotionLog, Long> {
    // Tìm log theo hành động (theo kế hoạch)
    List<PromotionLog> findByAction(String action);

    // Tìm log theo khoảng thời gian (theo kế hoạch)
    List<PromotionLog> findByLogTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
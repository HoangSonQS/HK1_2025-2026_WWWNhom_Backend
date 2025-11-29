package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Promotion;
import iuh.fit.se.sebook_backend.entity.PromotionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionLogRepository extends JpaRepository<PromotionLog, Long> {
    // Tìm log theo hành động (theo kế hoạch)
    List<PromotionLog> findByAction(String action);

    // Tìm log theo khoảng thời gian (theo kế hoạch)
    List<PromotionLog> findByLogTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT DISTINCT pl.promotion FROM PromotionLog pl WHERE pl.logTime BETWEEN :startTime AND :endTime")
    List<Promotion> findDistinctPromotionsByLogTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);
}
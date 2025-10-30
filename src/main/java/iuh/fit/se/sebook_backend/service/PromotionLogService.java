package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.PromotionLogResponseDTO;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Promotion;
import iuh.fit.se.sebook_backend.entity.PromotionLog;
import iuh.fit.se.sebook_backend.repository.PromotionLogRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionLogService {
    private final PromotionLogRepository promotionLogRepository;
    private final SecurityUtil securityUtil;

    public PromotionLogService(PromotionLogRepository promotionLogRepository, SecurityUtil securityUtil) {
        this.promotionLogRepository = promotionLogRepository;
        this.securityUtil = securityUtil;
    }

    /**
     * Phương thức nội bộ để tạo một bản ghi log
     */
    @Transactional
    public void createLog(Promotion promotion, String action) {
        Account actor = securityUtil.getLoggedInAccount();
        PromotionLog log = new PromotionLog();
        log.setPromotion(promotion);
        log.setActor(actor);
        log.setAction(action);
        log.setLogTime(LocalDateTime.now());
        promotionLogRepository.save(log);
    }

    /**
     * Lấy log theo hành động (theo kế hoạch)
     */
    @Transactional(readOnly = true)
    public List<PromotionLogResponseDTO> getLogsByAction(String action) {
        return promotionLogRepository.findByAction(action).stream()
                .map(this::toLogDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy log theo khoảng thời gian (theo kế hoạch)
     */
    @Transactional(readOnly = true)
    public List<PromotionLogResponseDTO> getLogsByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        return promotionLogRepository.findByLogTimeBetween(startDateTime, endDateTime).stream()
                .map(this::toLogDto)
                .collect(Collectors.toList());
    }

    private PromotionLogResponseDTO toLogDto(PromotionLog log) {
        return PromotionLogResponseDTO.builder()
                .promotionId(log.getPromotion().getId())
                .promotionCode(log.getPromotion().getCode())
                .actorName(log.getActor().getUsername())
                .action(log.getAction())
                .logTime(log.getLogTime())
                .build();
    }
}
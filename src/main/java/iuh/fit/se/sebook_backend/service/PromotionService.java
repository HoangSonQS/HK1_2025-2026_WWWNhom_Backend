package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.PromotionRequestDTO;
import iuh.fit.se.sebook_backend.dto.PromotionResponseDTO;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Promotion;
import iuh.fit.se.sebook_backend.entity.PromotionLog;
import iuh.fit.se.sebook_backend.repository.PromotionRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionLogService promotionLogService;
    private final SecurityUtil securityUtil;

    public PromotionService(PromotionRepository promotionRepository, PromotionLogService promotionLogService, SecurityUtil securityUtil) {
        this.promotionRepository = promotionRepository;
        this.promotionLogService = promotionLogService;
        this.securityUtil = securityUtil;
    }

    @Transactional
    public PromotionResponseDTO createPromotion(PromotionRequestDTO request) {
        // Kiểm tra mã có trùng không
        promotionRepository.findByCode(request.getCode()).ifPresent(p -> {
            throw new IllegalArgumentException("Promotion code already exists");
        });

        Account currentUser = securityUtil.getLoggedInAccount();
        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setCode(request.getCode());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setQuantity(request.getQuantity());
        promotion.setActive(true); // Mặc định là active (sau này có thể đổi thành false để chờ duyệt)
        promotion.setCreatedBy(currentUser);
        // approvedBy sẽ là null cho đến khi có người duyệt

        Promotion savedPromotion = promotionRepository.save(promotion);

        // Ghi log hành động tạo
        promotionLogService.createLog(savedPromotion, PromotionLog.CREATE);

        return toDto(savedPromotion);
    }

    @Transactional
    public PromotionResponseDTO approvePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        Account currentUser = securityUtil.getLoggedInAccount();
        promotion.setApprovedBy(currentUser);
        // Có thể thêm logic: promotion.setActive(true); nếu mặc định tạo là false

        Promotion savedPromotion = promotionRepository.save(promotion);

        // Ghi log hành động duyệt
        promotionLogService.createLog(savedPromotion, PromotionLog.APPROVE);

        return toDto(savedPromotion);
    }

    @Transactional
    public PromotionResponseDTO deactivatePromotion(Long id) { // Xóa mềm
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        promotion.setActive(false);
        Promotion savedPromotion = promotionRepository.save(promotion);

        // Ghi log hành động xóa mềm (hoặc từ chối)
        promotionLogService.createLog(savedPromotion, PromotionLog.DEACTIVATE);

        return toDto(savedPromotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> searchPromotions(String keyword) {
        return promotionRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(keyword, keyword)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDTO> getAllPromotions() {
        return promotionRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
        return toDto(promotion);
    }

    @Transactional(readOnly = true)
    public PromotionResponseDTO validatePromotionCode(String code) {
        Promotion promotion = promotionRepository
                .findByCodeAndIsActiveTrueAndEndDateAfter(code.trim(), LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired promotion code"));
        
        if (promotion.getQuantity() <= 0) {
            throw new IllegalStateException("Promotion code has been fully used");
        }
        
        return toDto(promotion);
    }

    private PromotionResponseDTO toDto(Promotion promotion) {
        return PromotionResponseDTO.builder()
                .id(promotion.getId())
                .name(promotion.getName())
                .code(promotion.getCode())
                .discountPercent(promotion.getDiscountPercent())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .quantity(promotion.getQuantity())
                .isActive(promotion.isActive())
                .createdByName(promotion.getCreatedBy() != null ? promotion.getCreatedBy().getUsername() : null)
                .approvedByName(promotion.getApprovedBy() != null ? promotion.getApprovedBy().getUsername() : null)
                .build();
    }
}
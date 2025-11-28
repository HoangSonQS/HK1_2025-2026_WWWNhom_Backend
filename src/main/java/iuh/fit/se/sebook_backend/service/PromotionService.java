package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.PromotionRequestDTO;
import iuh.fit.se.sebook_backend.dto.PromotionResponseDTO;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Promotion;
import iuh.fit.se.sebook_backend.entity.PromotionLog;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.PromotionRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionLogService promotionLogService;
    private final SecurityUtil securityUtil;
    private final NotificationService notificationService;
    private final AccountRepository accountRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PromotionService(PromotionRepository promotionRepository,
                            PromotionLogService promotionLogService,
                            SecurityUtil securityUtil,
                            NotificationService notificationService,
                            AccountRepository accountRepository) {
        this.promotionRepository = promotionRepository;
        this.promotionLogService = promotionLogService;
        this.securityUtil = securityUtil;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;
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
        promotion.setPriceOrderActive(request.getPriceOrderActive());
        promotion.setActive(false); // Mặc định chờ duyệt
        promotion.setStatus("PENDING");
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
        boolean wasPending = promotion.getApprovedBy() == null;
        promotion.setApprovedBy(currentUser);
        promotion.setActive(true);
        promotion.setStatus("ACTIVE");

        Promotion savedPromotion = promotionRepository.save(promotion);

        // Ghi log hành động duyệt
        promotionLogService.createLog(savedPromotion, PromotionLog.APPROVE);

        if (wasPending) {
            notifyActiveCustomers(savedPromotion, currentUser);
        }

        return toDto(savedPromotion);
    }

    @Transactional
    public PromotionResponseDTO deactivatePromotion(Long id) { // Từ chối
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        promotion.setActive(false);
        promotion.setStatus("REJECTED");
        Promotion savedPromotion = promotionRepository.save(promotion);

        // Ghi log hành động từ chối
        promotionLogService.createLog(savedPromotion, PromotionLog.REJECT);

        return toDto(savedPromotion);
    }

    @Transactional
    public PromotionResponseDTO pausePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        Account currentUser = securityUtil.getLoggedInAccount();
        promotion.setActive(false);
        promotion.setStatus("PAUSED");
        Promotion savedPromotion = promotionRepository.save(promotion);

        promotionLogService.createLog(savedPromotion, PromotionLog.PAUSE);
        notifyPromotionPaused(savedPromotion, currentUser);

        return toDto(savedPromotion);
    }

    @Transactional
    public PromotionResponseDTO resumePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));

        if (!"PAUSED".equalsIgnoreCase(promotion.getStatus())) {
            throw new IllegalStateException("Only paused promotions can be re-activated");
        }

        Account currentUser = securityUtil.getLoggedInAccount();
        promotion.setActive(true);
        promotion.setStatus("ACTIVE");
        Promotion savedPromotion = promotionRepository.save(promotion);

        promotionLogService.createLog(savedPromotion, PromotionLog.RESUME);
        notifyPromotionResumed(savedPromotion, currentUser);

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
                .priceOrderActive(promotion.getPriceOrderActive())
                .isActive(promotion.isActive())
                .status(promotion.getStatus())
                .createdByName(promotion.getCreatedBy() != null ? promotion.getCreatedBy().getUsername() : null)
                .approvedByName(promotion.getApprovedBy() != null ? promotion.getApprovedBy().getUsername() : null)
                .build();
    }

    private void notifyActiveCustomers(Promotion promotion, Account sender) {
        List<Account> activeAccounts = accountRepository.findByIsActiveTrue();
        if (activeAccounts == null || activeAccounts.isEmpty()) {
            return;
        }

        String title = "Khuyến mãi mới: " + promotion.getName();
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("Nhận ")
                .append(promotion.getDiscountPercent())
                .append("% giảm giá với mã ")
                .append(promotion.getCode())
                .append(". Áp dụng từ ")
                .append(formatDate(promotion.getStartDate()))
                .append(" đến ")
                .append(formatDate(promotion.getEndDate()));

        if (promotion.getPriceOrderActive() != null && promotion.getPriceOrderActive() > 0) {
            contentBuilder.append(". Đơn tối thiểu: ").append(formatCurrency(promotion.getPriceOrderActive()));
        }

        String content = contentBuilder.toString();

        activeAccounts.stream()
                .filter(account -> account.getRoles() != null && account.getRoles().stream()
                        .anyMatch(role -> "CUSTOMER".equalsIgnoreCase(role.getName())))
                .forEach(account ->
                        notificationService.createNotification(sender, account, title, content)
                );
    }

    private void notifyPromotionPaused(Promotion promotion, Account sender) {
        List<Account> activeAccounts = accountRepository.findByIsActiveTrue();
        if (activeAccounts == null || activeAccounts.isEmpty()) {
            return;
        }

        String title = "Khuyến mãi tạm ngưng: " + promotion.getName();
        String content = String.format("Khuyến mãi %s (mã %s) hiện đang tạm ngưng hoạt động. Vui lòng quay lại sau khi có thông báo mới.",
                promotion.getName(), promotion.getCode());

        activeAccounts.stream()
                .filter(account -> account.getRoles() != null && account.getRoles().stream()
                        .anyMatch(role -> "CUSTOMER".equalsIgnoreCase(role.getName())))
                .forEach(account ->
                        notificationService.createNotification(sender, account, title, content)
                );
    }

    private void notifyPromotionResumed(Promotion promotion, Account sender) {
        List<Account> activeAccounts = accountRepository.findByIsActiveTrue();
        if (activeAccounts == null || activeAccounts.isEmpty()) {
            return;
        }

        String title = "Khuyến mãi hoạt động lại: " + promotion.getName();
        String content = String.format("Khuyến mãi %s (mã %s) đã hoạt động trở lại. Đừng bỏ lỡ!",
                promotion.getName(), promotion.getCode());

        activeAccounts.stream()
                .filter(account -> account.getRoles() != null && account.getRoles().stream()
                        .anyMatch(role -> "CUSTOMER".equalsIgnoreCase(role.getName())))
                .forEach(account ->
                        notificationService.createNotification(sender, account, title, content)
                );
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(value);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DATE_FORMATTER);
    }
}
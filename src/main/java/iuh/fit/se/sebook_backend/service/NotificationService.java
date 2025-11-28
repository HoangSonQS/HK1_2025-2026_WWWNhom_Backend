package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.NotificationResponseDTO;
import iuh.fit.se.sebook_backend.entity.Account;
import iuh.fit.se.sebook_backend.entity.Notification;
import iuh.fit.se.sebook_backend.repository.NotificationRepository;
import iuh.fit.se.sebook_backend.utils.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityUtil securityUtil;

    public NotificationService(NotificationRepository notificationRepository, SecurityUtil securityUtil) {
        this.notificationRepository = notificationRepository;
        this.securityUtil = securityUtil;
    }

    /**
     * Lấy tất cả thông báo của người dùng đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.getId());

        return notifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     */
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId) {
        Account currentUser = securityUtil.getLoggedInAccount();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        // Đảm bảo người dùng chỉ có thể đánh dấu thông báo của chính mình
        if (!notification.getReceiver().getId().equals(currentUser.getId())) {
            throw new SecurityException("Cannot access this notification");
        }

        notification.setRead(true);
        Notification savedNotification = notificationRepository.save(notification);
        return toDto(savedNotification);
    }

    /**
     * Đếm số thông báo chưa đọc của người dùng hiện tại
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.getId());
        return notifications.stream()
                .filter(notification -> !notification.isRead())
                .count();
    }

    private NotificationResponseDTO toDto(Notification notification) {
        String senderName = "Hệ thống"; // Mặc định
        if (notification.getSender() != null) {
            senderName = notification.getSender().getUsername();
        }

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .senderName(senderName)
                .build();
    }

    /**
     * Phương thức nội bộ để hệ thống tạo thông báo.
     * Ví dụ: Gửi thông báo khi đơn hàng được giao.
     */
    @Transactional
    public void createNotification(Account sender, Account receiver, String title, String content) {
        Notification notification = new Notification();
        notification.setSender(sender); // 'sender' có thể là null nếu là thông báo hệ thống
        notification.setReceiver(receiver);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notificationRepository.save(notification);
    }
}
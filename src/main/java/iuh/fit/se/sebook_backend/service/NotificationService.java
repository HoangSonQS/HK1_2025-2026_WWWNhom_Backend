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

        // Trigger lazy loading để tránh LazyInitializationException
        Account receiver = notification.getReceiver();
        if (receiver == null) {
            throw new IllegalArgumentException("Notification receiver is null");
        }
        Long receiverId = receiver.getId();
        Long currentUserId = currentUser.getId();
        
        // Đảm bảo người dùng chỉ có thể đánh dấu thông báo của chính mình
        if (receiverId == null || currentUserId == null || !receiverId.equals(currentUserId)) {
            throw new SecurityException("Cannot access this notification");
        }

        notification.setRead(true);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Trigger lazy loading cho sender nếu có
        if (savedNotification.getSender() != null) {
            savedNotification.getSender().getUsername();
        }
        
        return toDto(savedNotification);
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
    @Transactional
    public void markAllAsRead() {
        Account currentUser = securityUtil.getLoggedInAccount();
        List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(currentUser.getId());
        
        notifications.stream()
                .filter(n -> !n.isRead())
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    /**
     * Đếm số thông báo chưa đọc của người dùng đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Account currentUser = securityUtil.getLoggedInAccount();
        Long receiverId = currentUser.getId();
        if (receiverId == null) {
            return 0;
        }
        return notificationRepository.countUnreadByReceiverId(receiverId);
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
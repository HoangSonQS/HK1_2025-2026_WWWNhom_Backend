package iuh.fit.se.sebook_backend.controller;

import iuh.fit.se.sebook_backend.dto.NotificationResponseDTO;
import iuh.fit.se.sebook_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lấy tất cả thông báo của người dùng đang đăng nhập.
     */
    @GetMapping("/my-notifications")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc.
     */
    @PutMapping("/mark-all-read")
    public ResponseEntity<String> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok("Tất cả thông báo đã được đánh dấu là đã đọc");
    }

    /**
     * Lấy số lượng thông báo chưa đọc.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }
}
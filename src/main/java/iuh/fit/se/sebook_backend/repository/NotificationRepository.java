package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Tìm tất cả thông báo cho một người nhận cụ thể,
     * sắp xếp theo thời gian tạo mới nhất.
     */
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
}
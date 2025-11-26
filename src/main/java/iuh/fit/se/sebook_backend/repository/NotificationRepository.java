package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Tìm tất cả thông báo cho một người nhận cụ thể,
     * sắp xếp theo thời gian tạo mới nhất.
     */
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    /**
     * Đếm số thông báo chưa đọc của một người nhận.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver.id = :receiverId AND n.isRead = false")
    long countUnreadByReceiverId(@Param("receiverId") Long receiverId);
}
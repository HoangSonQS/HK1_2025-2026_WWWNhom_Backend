package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountId(Long accountId);
    Optional<Order> findByPaymentCode(String paymentCode);
    /**
     * Tính tổng doanh thu của các đơn hàng đã hoàn thành
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    double sumTotalAmountByStatus(String status);

    /**
     * Đếm tổng số đơn hàng
     */
    long countByStatus(String status);

    /**
     * Đếm đơn hàng mới trong một khoảng thời gian
     */
    long countByOrderDateAfter(LocalDateTime after);
}
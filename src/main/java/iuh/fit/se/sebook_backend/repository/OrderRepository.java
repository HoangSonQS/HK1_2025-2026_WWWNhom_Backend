package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountId(Long accountId);
    
    /**
     * Tìm đơn hàng theo accountId với fetch join để load Address và OrderDetails
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.deliveryAddress " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.book " +
           "WHERE o.account.id = :accountId")
    List<Order> findByAccountIdWithDetails(@Param("accountId") Long accountId);
    
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

    /**
     * Tính tổng doanh thu trong một khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    double sumTotalAmountByStatusAndDateRange(String status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Đếm số đơn hàng trong một khoảng thời gian
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    long countByStatusAndDateRange(String status, LocalDateTime startDate, LocalDateTime endDate);
}
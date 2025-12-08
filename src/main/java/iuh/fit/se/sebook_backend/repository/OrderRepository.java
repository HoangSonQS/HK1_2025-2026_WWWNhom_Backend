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

    /**
     * Tính tổng doanh thu cho nhiều trạng thái trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    double sumTotalAmountByStatusesAndDateRange(@Param("statuses") List<String> statuses,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm số đơn hàng cho nhiều trạng thái trong khoảng thời gian
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    long countByStatusesAndDateRange(@Param("statuses") List<String> statuses,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy danh sách đơn hàng theo trạng thái và khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    List<Order> findByStatusAndDateRange(@Param("status") String status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}
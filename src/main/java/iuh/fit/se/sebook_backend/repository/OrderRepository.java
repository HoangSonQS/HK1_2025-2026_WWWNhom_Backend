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

    long countByAccountId(long accountId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.account.id = :accountId AND o.status = :status")
    double sumTotalAmountByAccountAndStatus(@Param("accountId") long accountId, @Param("status") String status);

    @Query("SELECT MAX(o.orderDate) FROM Order o WHERE o.account.id = :accountId")
    LocalDateTime findLastOrderDateByAccountId(@Param("accountId") long accountId);
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

    @Query("SELECT DATE(o.orderDate) AS day, COALESCE(SUM(o.totalAmount),0) AS revenue, COUNT(o) AS orders " +
            "FROM Order o WHERE o.status = :status AND o.orderDate >= :startDate AND o.orderDate < :endDate " +
            "GROUP BY DATE(o.orderDate) ORDER BY day")
    List<Object[]> sumRevenueByDay(@Param("status") String status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.orderDate >= :startDate AND o.orderDate < :endDate " +
            "GROUP BY o.status")
    List<Object[]> countStatusInRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.appliedPromotion.code, o.appliedPromotion.name, COUNT(o), SUM(o.totalAmount) " +
            "FROM Order o WHERE o.appliedPromotion IS NOT NULL AND o.status = 'COMPLETED' " +
            "GROUP BY o.appliedPromotion.code, o.appliedPromotion.name")
    List<Object[]> summaryByPromotion();

    @Query("SELECT o.account.id, o.account.username, o.account.email, COUNT(o), SUM(o.totalAmount) " +
            "FROM Order o WHERE o.appliedPromotion IS NOT NULL AND o.status = 'COMPLETED' " +
            "GROUP BY o.account.id, o.account.username, o.account.email " +
            "ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> topCustomersUsingPromotion();

    // Đơn hàng tổng tiền cao/thấp nhất
    Order findTopByOrderByTotalAmountDesc();
    Order findTopByOrderByTotalAmountAsc();
    
    // Fetch Order với orderDetails đã load
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.book " +
           "WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    // Đơn hàng có tổng số lượng sản phẩm cao/thấp nhất
    // Sử dụng native query để tránh lỗi PostgreSQL với DISTINCT + ORDER BY aggregate
    @Query(value = "SELECT o.* FROM orders o " +
           "LEFT JOIN order_details od ON o.id = od.order_id " +
           "GROUP BY o.id " +
           "ORDER BY COALESCE(SUM(od.quantity),0) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Order> findTopByTotalQuantityDescNative(@Param("limit") int limit);

    @Query(value = "SELECT o.* FROM orders o " +
           "LEFT JOIN order_details od ON o.id = od.order_id " +
           "GROUP BY o.id " +
           "ORDER BY COALESCE(SUM(od.quantity),0) ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Order> findTopByTotalQuantityAscNative(@Param("limit") int limit);
    
    // JPQL queries để load với orderDetails (sử dụng sau khi đã có order IDs)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderDetails od " +
           "LEFT JOIN FETCH od.book " +
           "WHERE o.id IN :orderIds")
    List<Order> findByIdsWithDetails(@Param("orderIds") List<Long> orderIds);
}
package iuh.fit.se.sebook_backend.repository;

import iuh.fit.se.sebook_backend.dto.TopSellingProductDTO;
import iuh.fit.se.sebook_backend.entity.OrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    /**
     * Lấy danh sách các sản phẩm bán chạy nhất,
     * chỉ tính các đơn hàng đã hoàn thành (COMPLETED).
     */
    @Query("SELECT new iuh.fit.se.sebook_backend.dto.TopSellingProductDTO(od.book.id, od.book.title, SUM(od.quantity)) " +
            "FROM OrderDetail od " +
            "WHERE od.order.status = 'COMPLETED' " +
            "GROUP BY od.book.id, od.book.title " +
            "ORDER BY SUM(od.quantity) DESC")
    List<TopSellingProductDTO> findTopSellingProducts(Pageable pageable);

    boolean existsByBook_Id(Long bookId);
}
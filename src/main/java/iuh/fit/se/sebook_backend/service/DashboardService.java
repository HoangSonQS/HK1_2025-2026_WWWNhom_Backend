package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.DashboardSummaryDTO;
import iuh.fit.se.sebook_backend.dto.TopSellingProductDTO;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.repository.AccountRepository;
import iuh.fit.se.sebook_backend.repository.OrderDetailRepository;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AccountRepository accountRepository;

    public DashboardService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository, AccountRepository accountRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Lấy các số liệu thống kê tổng quan
     */
    public DashboardSummaryDTO getDashboardSummary() {
        // Tổng doanh thu (chỉ tính đơn hàng đã hoàn thành)
        double totalRevenue = orderRepository.sumTotalAmountByStatus(Order.COMPLETED);

        // Tổng số đơn hàng (chỉ tính đơn hàng đã hoàn thành)
        long totalOrders = orderRepository.countByStatus(Order.COMPLETED);

        // Đơn hàng mới hôm nay (bất kể trạng thái)
        long newOrdersToday = orderRepository.countByOrderDateAfter(LocalDate.now().atStartOfDay());

        // Tài khoản mới hôm nay
        // Ghi chú: Cần thêm trường createdAt vào Account entity và phương thức countByCreatedAtAfter trong AccountRepository để có số liệu này.
        // Tạm thời để là 0.
        long newAccountsToday = 0; // Thay thế bằng: accountRepository.countByCreatedAtAfter(...)

        return DashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .newOrdersToday(newOrdersToday)
                .newAccountsToday(newAccountsToday)
                .build();
    }

    /**
     * Lấy top 5 sản phẩm bán chạy nhất
     */
    public List<TopSellingProductDTO> getTopSellingProducts() {
        // Lấy 5 sản phẩm đầu tiên
        return orderDetailRepository.findTopSellingProducts(PageRequest.of(0, 5));
    }
}
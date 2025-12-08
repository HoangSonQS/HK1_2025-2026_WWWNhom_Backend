package iuh.fit.se.sebook_backend.service;

import iuh.fit.se.sebook_backend.dto.DashboardSummaryDTO;
import iuh.fit.se.sebook_backend.dto.LowStockDTO;
import iuh.fit.se.sebook_backend.dto.RevenuePointDTO;
import iuh.fit.se.sebook_backend.dto.StatusCountDTO;
import iuh.fit.se.sebook_backend.dto.PromotionUsageDTO;
import iuh.fit.se.sebook_backend.dto.TopPromotionCustomerDTO;
import iuh.fit.se.sebook_backend.dto.TopSellingProductDTO;
import iuh.fit.se.sebook_backend.dto.InventorySummaryDTO;
import iuh.fit.se.sebook_backend.dto.InventoryCategoryDTO;
import iuh.fit.se.sebook_backend.dto.WarehouseSummaryDTO;
import iuh.fit.se.sebook_backend.entity.Order;
import iuh.fit.se.sebook_backend.repository.BookRepository;
import iuh.fit.se.sebook_backend.repository.OrderDetailRepository;
import iuh.fit.se.sebook_backend.repository.OrderRepository;
import iuh.fit.se.sebook_backend.repository.SupplierRepository;
import iuh.fit.se.sebook_backend.repository.ImportStockRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final BookRepository bookRepository;
    private final SupplierRepository supplierRepository;
    private final ImportStockRepository importStockRepository;

    public DashboardService(OrderRepository orderRepository,
                            OrderDetailRepository orderDetailRepository,
                            BookRepository bookRepository,
                            SupplierRepository supplierRepository,
                            ImportStockRepository importStockRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.bookRepository = bookRepository;
        this.supplierRepository = supplierRepository;
        this.importStockRepository = importStockRepository;
    }

    /**
     * Lấy các số liệu thống kê tổng quan
     */
    public DashboardSummaryDTO getDashboardSummary() {
        LocalDate now = LocalDate.now();
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPreviousMonth = startOfCurrentMonth;

        // Tổng doanh thu tháng này (chỉ tính đơn hàng đã hoàn thành)
        double totalRevenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                Order.COMPLETED, startOfCurrentMonth, now.plusDays(1).atStartOfDay());

        // Tổng số đơn hàng tháng này (chỉ tính đơn hàng đã hoàn thành)
        long totalOrders = orderRepository.countByStatusAndDateRange(
                Order.COMPLETED, startOfCurrentMonth, now.plusDays(1).atStartOfDay());

        // Doanh thu tháng trước
        double previousMonthRevenue = orderRepository.sumTotalAmountByStatusAndDateRange(
                Order.COMPLETED, startOfPreviousMonth, endOfPreviousMonth);

        // Số đơn hàng tháng trước
        long previousMonthOrders = orderRepository.countByStatusAndDateRange(
                Order.COMPLETED, startOfPreviousMonth, endOfPreviousMonth);

        // Tính % thay đổi
        double revenueChangePercent = previousMonthRevenue > 0
                ? ((totalRevenue - previousMonthRevenue) / previousMonthRevenue) * 100
                : (totalRevenue > 0 ? 100 : 0);

        double ordersChangePercent = previousMonthOrders > 0
                ? ((double)(totalOrders - previousMonthOrders) / previousMonthOrders) * 100
                : (totalOrders > 0 ? 100 : 0);

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
                .previousMonthRevenue(previousMonthRevenue)
                .previousMonthOrders(previousMonthOrders)
                .revenueChangePercent(revenueChangePercent)
                .ordersChangePercent(ordersChangePercent)
                .build();
    }

    /**
     * Lấy top 5 sản phẩm bán chạy nhất
     */
    public List<TopSellingProductDTO> getTopSellingProducts() {
        // Lấy 5 sản phẩm đầu tiên
        return orderDetailRepository.findTopSellingProducts(PageRequest.of(0, 5));
    }

    public List<RevenuePointDTO> getRevenueByDateRange(LocalDate start, LocalDate endExclusive) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = endExclusive.atStartOfDay();
        return orderRepository.sumRevenueByDay(Order.COMPLETED, startDt, endDt).stream()
                .map(row -> new RevenuePointDTO(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<StatusCountDTO> getStatusCounts(LocalDate start, LocalDate endExclusive) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = endExclusive.atStartOfDay();
        return orderRepository.countStatusInRange(startDt, endDt).stream()
                .map(row -> new StatusCountDTO((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    public List<LowStockDTO> getLowStock(int threshold) {
        return bookRepository.findLowStock(threshold).stream()
                .map(b -> new LowStockDTO(b.getId(), b.getTitle(), b.getQuantity()))
                .collect(Collectors.toList());
    }

    public List<PromotionUsageDTO> getPromotionUsage() {
        return orderRepository.summaryByPromotion().stream()
                .map(row -> new PromotionUsageDTO(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue(),
                        0 // discount sẽ được tính ở cấp service khác nếu cần subtotal
                ))
                .collect(Collectors.toList());
    }

    public List<TopPromotionCustomerDTO> getTopPromotionCustomers() {
        return orderRepository.topCustomersUsingPromotion().stream()
                .map(row -> new TopPromotionCustomerDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    public InventorySummaryDTO getInventorySummary() {
        var rows = bookRepository.sumInventory();
        if (rows.isEmpty()) return new InventorySummaryDTO(0, 0);
        Object[] r = rows.get(0);
        long totalQty = r[0] == null ? 0 : ((Number) r[0]).longValue();
        double totalValue = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
        return new InventorySummaryDTO(totalQty, totalValue);
    }

    public List<InventoryCategoryDTO> getInventoryByCategory() {
        return bookRepository.sumInventoryByCategory().stream()
                .map(row -> new InventoryCategoryDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Lấy các số liệu thống kê tổng quan cho warehouse
     */
    public WarehouseSummaryDTO getWarehouseSummary() {
        long totalBooks = bookRepository.count();
        long totalSuppliers = supplierRepository.count();
        long totalImportStocks = importStockRepository.count();
        return new WarehouseSummaryDTO(totalBooks, totalSuppliers, totalImportStocks);
    }
}
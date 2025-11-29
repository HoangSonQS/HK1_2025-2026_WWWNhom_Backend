package iuh.fit.se.sebook_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DashboardSummaryDTO {
    private double totalRevenue; // Tổng doanh thu
    private long totalOrders; // Tổng số đơn hàng
    private long newOrdersToday; // Đơn hàng mới hôm nay
    private long newAccountsToday; // Khách hàng mới hôm nay
    
    // So sánh với tháng trước
    private double previousMonthRevenue; // Doanh thu tháng trước
    private long previousMonthOrders; // Số đơn hàng tháng trước
    private double revenueChangePercent; // % thay đổi doanh thu
    private double ordersChangePercent; // % thay đổi số đơn hàng
}
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
}
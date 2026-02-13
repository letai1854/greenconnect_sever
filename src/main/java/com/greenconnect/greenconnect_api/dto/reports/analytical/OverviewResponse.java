package com.greenconnect.greenconnect_api.dto.reports.analytical;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Tab Overview - Tổng quan Tài chính
 * ✅ Chuyển sang Double để tương thích tốt hơn với Frontend Charts
 * Đồng bộ với frontend: OverviewTab
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewResponse {
    
    // ==================== KPI Cards ====================
    private Double totalRevenue;           // Tổng doanh thu
    private Integer totalOrders;           // Tổng đơn hàng
    private Double aov;                    // AOV - Giá trị ĐH TB (Average Order Value)
    private Double avgDailyRevenue;        // Doanh thu TB/Ngày
    private Integer daysInRange;           // Số ngày trong khoảng thời gian
    
    // ==================== Financial Chart Data ====================
    private List<MonthlyRevenueData> monthlyRevenueData;  // Dữ liệu biểu đồ theo tháng
    
    // ==================== Detailed Data Table ====================
    private List<DailyOperationData> dailyOperationData;  // Bảng đối soát hằng ngày
    
    /**
     * Dữ liệu doanh thu & AOV theo tháng cho biểu đồ
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueData {
        private String month;              // "T1", "T2", ... "T12"
        private Double revenue;            // Doanh thu tháng
        private Integer orderCount;        // Số đơn hàng
        private Double aov;                // AOV = revenue / orderCount
    }
    
    /**
     * Dữ liệu vận hành hằng ngày cho bảng chi tiết
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOperationData {
        private LocalDate date;                  // Ngày
        private Integer orderCount;              // Số đơn hàng
        private Double revenue;                  // Doanh thu
        private Double aov;                      // AOV
        private Double contributionRate;         // Tỷ trọng đóng góp (%)
    }
}

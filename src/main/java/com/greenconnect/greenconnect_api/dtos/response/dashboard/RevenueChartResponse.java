package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho biểu đồ doanh thu 30 ngày
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartResponse {
    
    private List<DailyRevenue> dailyRevenues;
    private BigDecimal totalRevenue;        // Tổng doanh thu 30 ngày
    private BigDecimal averageRevenue;      // Trung bình mỗi ngày
    private BigDecimal maxRevenue;          // Doanh thu cao nhất
    private LocalDate maxRevenueDate;       // Ngày có doanh thu cao nhất
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private BigDecimal revenue;
        private Long orderCount;
    }
}

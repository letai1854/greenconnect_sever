package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho phần so sánh hiệu suất giữa 2 tuần
 * Dùng cho các card: Doanh thu, Đơn hàng, Khách hàng mới, Đánh giá
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonStatsResponse {
    
    // Doanh thu
    private BigDecimal currentRevenue;      // Doanh thu tuần này
    private BigDecimal previousRevenue;     // Doanh thu tuần trước
    private Double revenueTrend;            // % thay đổi
    
    // Đơn hàng
    private Long currentOrders;             // Số đơn tuần này
    private Long previousOrders;            // Số đơn tuần trước
    private Double ordersTrend;             // % thay đổi
    
    // Khách hàng mới
    private Long currentNewCustomers;       // Khách mới tuần này
    private Long previousNewCustomers;      // Khách mới tuần trước
    private Double newCustomersTrend;       // % thay đổi
    
    // Đánh giá trung bình
    private Double currentAverageRating;    // Rating tuần này
    private Double previousAverageRating;   // Rating tuần trước
    private Double ratingTrend;             // % thay đổi
}

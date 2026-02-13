package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.util.List;

import com.greenconnect.greenconnect_api.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho biểu đồ phân bố trạng thái đơn hàng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChartResponse {
    
    private List<StatusCount> statusCounts;
    private Long totalOrders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private OrderStatus status;
        private String statusLabel;     // Tên hiển thị tiếng Việt
        private Long count;
        private Double percentage;      // % so với tổng
        private String color;           // Màu cho chart
    }
}

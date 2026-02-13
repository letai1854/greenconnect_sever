package com.greenconnect.greenconnect_api.dtos.response.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessTargetsResponse {
    
    private String period;  // "11/2024"
    private List<TargetData> targets;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetData {
        private String id;          // revenue, orders, new_customers, five_star_reviews
        private String name;        // Tên hiển thị
        private BigDecimal current; // Giá trị hiện tại
        private BigDecimal target;  // Mục tiêu
        private String unit;        // Đơn vị
        private Double percentage;  // % hoàn thành
        private String color;       // Mã màu hex
    }
}

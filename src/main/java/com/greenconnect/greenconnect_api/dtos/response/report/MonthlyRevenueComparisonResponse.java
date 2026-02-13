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
public class MonthlyRevenueComparisonResponse {
    
    private int year;
    private int compareYear;
    private List<MonthlyRevenue> data;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;       // T1, T2, ...
        private int monthNumber;    // 1-12
        private BigDecimal thisYear;
        private BigDecimal lastYear;
        private Double growthRate;  // % tăng trưởng
    }
}

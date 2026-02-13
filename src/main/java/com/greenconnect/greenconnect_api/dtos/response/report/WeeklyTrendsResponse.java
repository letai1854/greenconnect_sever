package com.greenconnect.greenconnect_api.dtos.response.report;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTrendsResponse {
    
    private String period;  // "21/11/2024 - 27/11/2024"
    private List<Double> orderTrend;        // 7 ngày
    private List<Double> revenueTrend;      // Đơn vị: triệu VNĐ
    private List<Double> newCustomerTrend;
    private List<Double> ratingTrend;
    private List<String> labels;            // ["T5", "T6", ...]
}

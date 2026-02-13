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
public class OrdersHeatmapResponse {
    
    private List<String> days;      // ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]
    private List<Integer> hours;    // [0, 1, 2, ..., 23]
    private Integer maxOrders;      // Giá trị max để scale màu
    private List<HeatmapDay> heatmapData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapDay {
        private Integer dayIndex;       // 0 = Thứ 2, 6 = Chủ nhật
        private String dayName;         // T2, T3, ...
        private List<Integer> hourlyData; // 24 phần tử
    }
}

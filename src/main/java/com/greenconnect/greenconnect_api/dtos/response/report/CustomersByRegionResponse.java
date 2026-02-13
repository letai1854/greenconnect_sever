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
public class CustomersByRegionResponse {
    
    private Long totalCustomers;
    private List<RegionData> regions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionData {
        private String region;      // Tên tỉnh/thành phố
        private Long count;         // Số khách hàng
        private Double percentage;  // Phần trăm
    }
}

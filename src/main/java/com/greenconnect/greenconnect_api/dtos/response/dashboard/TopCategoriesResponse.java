package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho Top 5 danh mục có doanh số cao nhất
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCategoriesResponse {
    
    private List<TopCategory> categories;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategory {
        private UUID categoryId;
        private String categoryName;
        private String categoryImage;
        private Long productCount;          // Số sản phẩm trong danh mục
        private Long quantitySold;          // Tổng số lượng bán
        private BigDecimal revenue;         // Tổng doanh thu
    }
}

package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho Top 5 sản phẩm bán chạy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductsResponse {
    
    private List<TopProduct> products;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private UUID productId;
        private String productName;
        private String productImage;
        private String categoryName;
        private Long quantitySold;          // Số lượng đã bán
        private BigDecimal revenue;         // Doanh thu từ sản phẩm
        private Double averageRating;       // Rating trung bình
    }
}

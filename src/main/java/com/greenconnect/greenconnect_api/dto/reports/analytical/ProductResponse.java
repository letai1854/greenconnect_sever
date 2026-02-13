package com.greenconnect.greenconnect_api.dto.reports.analytical;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Tab Product - Phân tích Sản phẩm
 * ✅ Chuyển sang Double để tương thích tốt hơn với Frontend Charts
 * Đồng bộ với frontend: ProductTab
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    
    // ==================== Top Sản phẩm Bán chạy ====================
    private List<TopProductData> topProducts;       // Top 3 sản phẩm bán chạy
    
    // ==================== Sản phẩm có Tỷ lệ Hoàn trả Cao ====================
    private List<ReturnedProductData> returnedProducts;  // Top 3 sản phẩm hoàn trả cao
    
    /**
     * Dữ liệu sản phẩm bán chạy
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductData {
        private UUID productId;
        private String productName;
        private String productImage;
        private String categoryName;
        private Double revenue;              // Doanh thu
        private Integer quantitySold;        // Số lượng đã bán
        private Double averageRating;        // Đánh giá trung bình
    }
    
    /**
     * Dữ liệu sản phẩm có tỷ lệ hoàn trả cao (Bad Performers)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnedProductData {
        private UUID productId;
        private String productName;
        private String productImage;
        private Double returnRate;           // Tỷ lệ hoàn trả (%)
        private Integer returnCount;         // Số lần hoàn trả
        private String topReturnReason;      // Lý do hoàn trả chính
        private Integer totalOrders;         // Tổng số đơn hàng của sản phẩm
    }
}

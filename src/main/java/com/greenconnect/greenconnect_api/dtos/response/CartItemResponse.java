package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho Cart Item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    
    private UUID id;
    private Integer quantity;
    private LocalDateTime addedDate;
    private LocalDateTime updatedDate;
    
    // Thông tin product variant
    private ProductVariantInfo productVariant;
    
    // Tính toán giá
    private BigDecimal totalPrice;        // quantity * price (giá gốc)
    private BigDecimal totalDiscountedPrice; // quantity * discounted price (giá sau giảm)
    private BigDecimal totalDiscountAmount;  // Tổng số tiền được giảm
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantInfo {
        private UUID id;
        private String name;
        private String sku;
        private BigDecimal price;
        private BigDecimal discountPercentage; // % giảm giá
        private BigDecimal discountedPrice;   // Giá sau khi giảm
        private BigDecimal discountAmount;    // Số tiền được giảm
        private Integer stockQuantity;
        private String unit;
        private String mainImageUrl;
        private Boolean isActive;
        
        // Thông tin product
        private ProductInfo product;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductInfo {
            private UUID id;
            private String name;
            private String slug;
            private Boolean isActive;
        }
    }
}
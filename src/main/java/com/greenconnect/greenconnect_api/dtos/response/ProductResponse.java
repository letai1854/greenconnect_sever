package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ProductResponse {
    
    private UUID id;
    private String name;
    private String description;
    private String slug;
    private Boolean isActive;
    private Boolean isFeatured;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Long sellNumber; // Số lượng đã bán (optional - có thể null cho backward compatibility)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Min and max price across variants after applying discountPercentage
    private java.math.BigDecimal minValue;
    private java.math.BigDecimal maxValue;
    
    // Category info
    private CategoryInfo category;
    
    // Supplier info
    private SupplierInfo supplier;
    
    // ⭐ Main image URL (ảnh chính của product - isMain = true)
    private String mainImageUrl;
    
    // ⭐ Product images (dùng chung cho tất cả variants)
    private List<ProductImageInfo> images;
    
    // Product variants
    private List<ProductVariantInfo> variants;
    
    // ⭐ Favorite status (runtime - tính theo userId)
    private Boolean isFavorited; // Sản phẩm có trong danh sách yêu thích của user hiện tại không
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter

    public static class CategoryInfo {
        private UUID id;
        private String name;
        private String imageUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter

    public static class SupplierInfo {
        private UUID id;
        private String name;
        private String contactEmail;
        private String phoneNumber;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter

    public static class ProductVariantInfo {
        private UUID id;
        private String name;
        private String sku;
        private BigDecimal price;
        private BigDecimal discountPercentage; // % giảm giá
        private BigDecimal discountedPrice;   // Giá sau khi giảm (calculated)
        // Min and max values for variant price after applying discount percentage (if discountPercentage != 0)
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private BigDecimal discountAmount;    // Số tiền được giảm (calculated)
        private Integer stockQuantity;
        private String unit;
        // ⚠️ mainImageUrl đã chuyển lên Product level (dùng chung)
        // private String mainImageUrl; // REMOVED - use product.mainImageUrl instead
        private Boolean isActive;
        private Boolean isDefault;
        
        // ⚠️ Images được lấy từ Product level, không cần ở đây nữa
        // private List<ProductImageInfo> images; // REMOVED - use product.images instead
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter

    public static class ProductImageInfo {
        private UUID id;
        private String mediaType;
        private String mediaUrl;
        private Integer displayOrder;
        private Boolean isMain; // Đánh dấu ảnh chính của product
    }
}
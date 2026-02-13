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
public class ProductWithTopReviewResponse {
    
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
    private BigDecimal minValue;
    private BigDecimal maxValue;
    
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
    private Boolean isFavorited;
    
    // ⭐ Top review with media and user info (NULL if no reviews)
    private TopReviewInfo topReview;
    
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
        private BigDecimal discountPercentage;
        private BigDecimal discountedPrice;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private BigDecimal discountAmount;
        private Integer stockQuantity;
        private String unit;
        private Boolean isActive;
        private Boolean isDefault;
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
        private Boolean isMain;
    }
    
    /**
     * Top review info with media and user details
     * Chứa đánh giá cao nhất (theo rating) có hình ảnh
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    public static class TopReviewInfo {
        private UUID id;
        private short rating;
        private String comment;
        private LocalDateTime reviewTime;
        
        // Người viết review
        private UserMinimalInfo user;
        
        // Danh sách hình ảnh/video trong review (sorted by displayOrder)
        private List<ReviewMediaInfo> mediaList;
        
        // Trả lời của cửa hàng (nếu có)
        private ShopReplyInfo shopReply;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    public static class UserMinimalInfo {
        private UUID userId;
        private String fullName;
        private String avatarUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    public static class ReviewMediaInfo {
        private UUID id;
        private String mediaType;
        private String mediaUrl;
        private Integer displayOrder;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    public static class ShopReplyInfo {
        private String content;
        private LocalDateTime repliedAt;
        private String replierName;
    }
}

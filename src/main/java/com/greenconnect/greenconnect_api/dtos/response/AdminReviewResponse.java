package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO cho Admin xem danh sách review với thông tin sản phẩm đầy đủ
 */
@Data
@Builder
public class AdminReviewResponse {
    
    // Thông tin sản phẩm
    private ProductInfo product;
    
    // Thông tin đánh giá
    private ReviewInfo review;
    
    // Thông tin người dùng
    private UserInfo user;
    
    // Phản hồi của shop (nếu có)
    private ShopReplyInfo shopReply;
    
    @Data
    @Builder
    public static class ProductInfo {
        private UUID productId;
        private String productName;
        private String productImageUrl;
        private BigDecimal price;
        private String unit;
    }
    
    @Data
    @Builder
    public static class ReviewInfo {
        private UUID reviewId;
        private short rating;
        private String comment;
        private LocalDateTime reviewTime;
        private List<MediaInfo> mediaList;
    }
    
    @Data
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String userName;
        private String avatarUrl;
    }
    
    @Data
    @Builder
    public static class ShopReplyInfo {
        private String content;
        private LocalDateTime repliedAt;
        private String replierName;
    }
    
    @Data
    @Builder
    public static class MediaInfo {
        private String mediaType;
        private String mediaUrl;
    }
}

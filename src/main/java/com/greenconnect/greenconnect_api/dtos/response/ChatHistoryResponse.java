package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response cho API load lịch sử chat
 * Bao gồm cả thông tin sản phẩm đã gợi ý (để frontend hiển thị lại)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    
    private UUID sessionId;
    private String sessionTitle;
    private List<ChatMessageDto> messages;
    
    // 🔥 Pagination info
    private int currentPage;
    private int pageSize;
    private long totalMessages;
    private int totalPages;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private UUID id;
        private String sender;              // "USER" hoặc "BOT"
        private String content;             // Nội dung text của tin nhắn
        private LocalDateTime createdAt;
        
        /**
         * Danh sách sản phẩm đã gợi ý (chỉ có khi sender = BOT)
         * Frontend hiển thị lại product cards với ảnh, giá...
         */
        private List<ProductSuggestion> suggestedProducts;
    }
    
    /**
     * Thông tin sản phẩm đơn giản (dùng trong lịch sử chat)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestion {
        private UUID id;
        private UUID defaultVariantId;  // 🔥 ID của variant mặc định (để frontend add to cart)
        private String name;
        private String mainImage;       // URL ảnh chính
        private String priceRange;      // VD: "50.000₫ - 100.000₫"
        private Double rating;
        private Long sold;
    }
}

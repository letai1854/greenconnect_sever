package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private UUID sessionId;           // 🔥 Session ID để frontend lưu lại cho lần chat tiếp theo
    private String botMessage;
    private List<ProductSuggestion> products;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSuggestion {
        private UUID id;
        private UUID defaultVariantId;  // 🔥 ID của variant mặc định (để frontend add to cart)
        private String name;
        private String mainImage;
        private String priceRange;
        private Double rating;
        private Long sold;
        private String supplierName;
    }
}

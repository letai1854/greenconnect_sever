package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    
    private UUID id;
    private UUID productVariantId;
    private UUID productId;           // ⭐ ID của product để kiểm tra review
    private Integer quantity;
    
    // ========== PRODUCT SNAPSHOT ==========
    private String productName;
    private String productImageUrl;
    private String unit;
    private BigDecimal originalPricePerUnit;
    private BigDecimal sellingPricePerUnit;
    private BigDecimal discountPercentage;
    
    // ========== CALCULATED FIELDS ==========
    private BigDecimal totalPrice; // sellingPricePerUnit * quantity
    private BigDecimal totalOriginalPrice; // originalPricePerUnit * quantity
    private BigDecimal totalDiscount; // totalOriginalPrice - totalPrice
    
    // ========== REVIEW DATA (for DA_DANH_GIA orders) ==========
    // ⭐ Tất cả reviews của cùng productId (không chỉ review của orderDetail này)
    // Bao gồm: user message + shop reply + media từ nhiều người mua khác nhau
    private List<ProductReviewResponse> reviews;
    
    // ⭐ Kiểm tra xem orderDetail này (sản phẩm trong đơn hàng này) đã được đánh giá chưa
    private Boolean hasReviewed;      // true nếu orderDetail này đã có review
}
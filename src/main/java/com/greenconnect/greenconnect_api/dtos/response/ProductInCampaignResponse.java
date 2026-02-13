package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response cho danh sách sản phẩm trong campaign với thông tin đã được thêm vào campaign hay chưa
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
public class ProductInCampaignResponse extends ProductResponse {
    
    /**
     * ⭐ Sản phẩm này đã được thêm vào campaign hay chưa
     */
    private Boolean isInCampaign;
    
    /**
     * UUID của PromotionProduct nếu sản phẩm đã trong campaign
     */
    private UUID promotionProductId;
    
    /**
     * Discount riêng của product trong campaign (nếu có override)
     */
    private BigDecimal productDiscountValue;
    
    // Constructor từ ProductResponse + isInCampaign flag
    public ProductInCampaignResponse(ProductResponse productResponse, Boolean isInCampaign, 
                                    UUID promotionProductId, BigDecimal productDiscountValue) {
        super(
            productResponse.getId(),
            productResponse.getName(),
            productResponse.getDescription(),
            productResponse.getSlug(),
            productResponse.getIsActive(),
            productResponse.getIsFeatured(),
            productResponse.getAverageRating(),
            productResponse.getReviewCount(),
            productResponse.getSellNumber(), // ⚡ Số lượng đã bán
            productResponse.getCreatedAt(),
            productResponse.getUpdatedAt(),
            productResponse.getMinValue(),
            productResponse.getMaxValue(),
            productResponse.getCategory(),
            productResponse.getSupplier(),
            productResponse.getMainImageUrl(),
            productResponse.getImages(),
            productResponse.getVariants(),
            productResponse.getIsFavorited()
        );
        this.isInCampaign = isInCampaign;
        this.promotionProductId = promotionProductId;
        this.productDiscountValue = productDiscountValue;
    }
}

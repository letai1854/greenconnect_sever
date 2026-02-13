package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.greenconnect.greenconnect_api.enums.CampaignType;
import com.greenconnect.greenconnect_api.enums.DiscountType;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class PromotionCampaignUpdateRequest {
    private String campaignName;
    
    private String description;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private Boolean isActive;
    
    private String slug;
    
    private String coverBannerUrl;
    
    private String urlViewAll;
    
    private CampaignType campaignType;
    
    private Integer displayOrder;
    
    private java.math.BigDecimal discountPercentage; // % giảm giá chung cho campaign (0-100)

    @Valid
    @JsonProperty("promotionProducts")
    private List<ProductDiscountItem> productsToAdd; // add new products (no ids on promotion-product)

    @Data
    public static class ProductDiscountItem {
        private UUID productId;
        private DiscountType discountType;
        private BigDecimal discountValue;
    }
}

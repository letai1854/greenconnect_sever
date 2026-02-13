package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.CampaignType;
import com.greenconnect.greenconnect_api.enums.DiscountType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromotionCampaignResponse {
    private UUID id;
    private String campaignName;
    private String description;
    private String slug;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private String coverBannerUrl;
    private String urlViewAll;
    private CampaignType campaignType;
    private Integer displayOrder;
    private java.math.BigDecimal discountPercentage; // % giảm giá chung cho campaign
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductDiscountResponse> promotionProducts;

    @Data
    @Builder
    public static class ProductDiscountResponse {
        private UUID productId;
        private DiscountType discountType;
        private BigDecimal discountValue;
    }
}
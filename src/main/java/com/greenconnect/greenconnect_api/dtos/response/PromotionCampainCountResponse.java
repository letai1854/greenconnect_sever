package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.CampaignType;
import com.greenconnect.greenconnect_api.enums.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCampainCountResponse {
    private UUID id;
    private String campaignName;
    private String description;
    private String slug;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String coverBannerUrl;
    private String urlViewAll;
    private CampaignType campaignType;
    private Integer displayOrder;
    private BigDecimal discountPercentage; 
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ⭐ Số lượng products trong campaign
    private Integer productCount;
}

package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_products", indexes = {
    // TỐT: Để tìm tất cả product_id thuộc một campaign_id. Phục vụ lazy loading.
    @Index(name = "idx_promotion_product_campaign", columnList = "campaign_id"),
    // TỐT: Để tìm tất cả campaign_id mà một product_id tham gia.
    @Index(name = "idx_promotion_product_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionProduct {
    
    @EmbeddedId
    private PromotionProductId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("campaignId")
    @JoinColumn(name = "campaign_id")
    private PromotionCampaign campaign;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "discount_type", length = 50)
    private String discountType;
    
    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;
}
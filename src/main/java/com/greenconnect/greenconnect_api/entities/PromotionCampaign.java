package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.greenconnect.greenconnect_api.enums.CampaignType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotion_campaigns", indexes = {
    // Giữ nguyên các index hiện tại của bạn, chúng đều có giá trị.
    @Index(name = "idx_campaign_active_dates", columnList = "is_active, start_date, end_date"),
    @Index(name = "idx_campaign_slug", columnList = "slug", unique = true), // Đảm bảo có `unique=true` ở đây
    @Index(name = "idx_campaign_dates", columnList = "start_date, end_date"),
    // ✅ NEW: Tối ưu cho FLASH_SALE query (covering index)
    // Thứ tự: campaignType (high selectivity) -> isActive -> dates -> createdAt
    // ⚠️ LƯU Ý: MySQL không hỗ trợ DESC trong @Index annotation
    //           Nhưng MySQL optimizer vẫn có thể reverse scan index cho ORDER BY DESC
    @Index(name = "idx_campaign_flash_sale", 
           columnList = "campaign_type, is_active, start_date, end_date, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCampaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "campaign_name", nullable = false, length = 255)
    private String campaignName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "slug", unique = true, nullable = false, length = 255)
    private String slug;

    @Column(name = "cover_banner_url", length = 255)
    private String coverBannerUrl;

    @Column(name = "url_view_all", length = 255)
    private String urlViewAll;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type")
    private CampaignType campaignType;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal discountPercentage; // % giảm giá chung cho campaign (0-100)
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ===== RELATIONSHIPS =====
    
    // 1 Campaign : Many PromotionProducts (OneToMany)
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PromotionProduct> promotionProducts;
    
    // 1 Campaign : Many HomepageLayouts (ManyToMany)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "homepage_layout_campaigns",
        joinColumns = @JoinColumn(name = "campaign_id"),
        inverseJoinColumns = @JoinColumn(name = "homepage_layout_id"),
        indexes = {
            @Index(name = "idx_hlc_campaign", columnList = "campaign_id"),
            @Index(name = "idx_hlc_layout", columnList = "homepage_layout_id")
        }
    )
    private List<HomepageLayout> homepageLayouts;
}
package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.greenconnect.greenconnect_api.enums.WidgetType;
import com.greenconnect.greenconnect_api.enums.DisplayLayoutType;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Index;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "homepage_layouts", indexes = {
    // CRITICAL: Index này phục vụ trực tiếp cho truy vấn tải trang chủ.
    // Nó giúp tìm nhanh các layout active.
    @Index(name = "idx_layout_active", columnList = "is_active")
})
public class HomepageLayout {

    @Id     
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // Optional human-friendly name for the layout (can be useful in admin)
    @Column(name = "title", length = 255)
    private String title;

    // The banner group shown on the homepage (only one banner group is used)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_group_id")
    private BannerGroup bannerGroup;

    // Ordered list of promotion campaigns to show on homepage.
    // We use a join table so campaigns are reusable and their order can be controlled.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "homepage_layout_campaigns",
        joinColumns = @JoinColumn(name = "homepage_layout_id"),
        inverseJoinColumns = @JoinColumn(name = "campaign_id")
    )
    @OrderColumn(name = "display_order")
    private java.util.List<PromotionCampaign> campaigns;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Index;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "banners", indexes = {
    // CRITICAL: Index tổng hợp để tìm và sắp xếp tất cả banner đang hoạt động
    // trong một nhóm cụ thể. Đây là truy vấn rất phổ biến.
    @Index(name = "idx_banner_group_active_order", columnList = "banner_group_id, is_active, display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "banner_name", nullable = false, length = 255)
    private String bannerName;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "target_url", length = 255)
    private String targetUrl;

    // Bỏ cột 'position'
    // Thay bằng liên kết tới BannerGroup
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_group_id")
    private BannerGroup bannerGroup;

    // Thứ tự hiển thị của banner NÀY trong nhóm của nó
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

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
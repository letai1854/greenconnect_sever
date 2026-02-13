package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.Index;
import com.greenconnect.greenconnect_api.enums.BannerGroupLayoutType;

@Entity
@Table(name = "banner_groups", indexes = {
    // CRITICAL: Để tìm kiếm nhanh một nhóm banner bằng key duy nhất của nó.
    // group_key đã có `unique=true` nên DB thường tự tạo index, nhưng khai báo tường minh sẽ tốt hơn.
    @Index(name = "idx_bannergroup_key", columnList = "group_key", unique = true)
})@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_name", nullable = false, length = 255)
    private String groupName;

    // Key này sẽ được dùng trong HomepageLayout.dataSource để liên kết
    @Column(name = "group_key", unique = true, nullable = false, length = 100)
    private String groupKey;

    // Quyết định cách các banner trong nhóm này được sắp xếp
    @Enumerated(EnumType.STRING)
    @Column(name = "display_layout", nullable = false, length = 50)
    private BannerGroupLayoutType displayLayout;

    @OneToMany(mappedBy = "bannerGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Banner> banners;
}


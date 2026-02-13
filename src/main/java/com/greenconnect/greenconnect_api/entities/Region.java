package com.greenconnect.greenconnect_api.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Đại diện cho một khu vực địa lý (VD: Đông Nam Bộ, Bắc Trung Bộ, v.v.)
 */
@Entity
@Table(name = "regions", indexes = {
    @Index(name = "idx_region_slug", columnList = "slug", unique = true),
    @Index(name = "idx_region_active", columnList = "is_active"),
    // ✅ NEW: Join optimization (RegionAddress queries)
    @Index(name = "idx_region_active_sort", columnList = "is_active, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name; // VD: Khu vực Đông Nam Bộ
    
    @Column(name = "slug", nullable = false, length = 255, unique = true)
    private String slug;
    
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RegionAddress> regionAddresses = new ArrayList<>();
}

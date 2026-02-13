package com.greenconnect.greenconnect_api.entities;

import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.AddressType;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Địa chỉ cụ thể trong một khu vực
 * Chứa thông tin về phường/quận/thành phố
 */
@Entity
@Table(name = "region_addresses", indexes = {
    @Index(name = "idx_region_address_region", columnList = "region_id"),
    @Index(name = "idx_region_address_type", columnList = "address_type"),
    @Index(name = "idx_region_address_active", columnList = "is_active"),
    // ✅ NEW: Join optimization (region with addresses)
    @Index(name = "idx_region_address_region_active", columnList = "region_id, is_active"),
    // ✅ NEW: Sort by order
    @Index(name = "idx_region_address_region_sort", columnList = "region_id, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
    
    @Column(name = "full_address", nullable = false, columnDefinition = "TEXT")
    private String fullAddress;
    
    @Column(name = "ward", length = 100)
    private String ward; // Phường cũ (VD: Tân Thủy)
    
    @Column(name = "district", length = 100)
    private String district; // Quận cũ (VD: Ba Tri)
    
    @Column(name = "city", length = 100)
    private String city; // Tỉnh cũ (VD: Bến Tre)
    
    @Column(name = "ward_new", length = 100)
    private String wardNew; // Phường mới (VD: Tân Thủy)
    
    @Column(name = "city_new", length = 100)
    private String cityNew; // Tỉnh mới (VD: Vĩnh Long)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default
    private AddressType addressType = AddressType.CU63; // MOI (mới) hoặc CU (cũ)
    
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

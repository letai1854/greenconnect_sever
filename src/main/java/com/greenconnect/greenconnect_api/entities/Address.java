package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.greenconnect.greenconnect_api.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses", indexes = {
    // ✅ Critical: Lấy addresses của user (checkout, profile)
    @Index(name = "idx_address_user", columnList = "user_id"),
    // ✅ Important: Default address lookup
    @Index(name = "idx_address_user_default", columnList = "user_id, is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Read-only FK convenience field so services can access the owner id
    // without initializing the full User proxy. This mirrors the
    // 'user_id' column but is not insertable/updatable by JPA.
    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    private UUID userId;
    
    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;
    
    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;
    
    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;
    
    @Column(name = "note", length = 255)
    private String note;
    
    // ========== Vietnam Address (63 provinces) - All nullable ==========
    
    @Column(name = "province_code_63", length = 10)
    private String provinceCode63; // Mã tỉnh/thành phố (63 tỉnh)
    
    @Column(name = "province_name_63", length = 100)
    private String provinceName63; // Tên tỉnh/thành phố
    
    @Column(name = "district_code_63", length = 10)
    private String districtCode63; // Mã quận/huyện
    
    @Column(name = "district_name_63", length = 100)
    private String districtName63; // Tên quận/huyện
    
    @Column(name = "ward_code_63", length = 10)
    private String wardCode63; // Mã phường/xã
    
    @Column(name = "ward_name_63", length = 100)
    private String wardName63; // Tên phường/xã
    
    // ========== Vietnam Address (34 provinces - old system) ==========
    
    @Column(name = "province_code_34", length = 10)
    private String provinceCode34; // Mã tỉnh/thành phố (34 tỉnh - hệ thống cũ)
    
    @Column(name = "province_name_34", length = 100)
    private String provinceName34; // Tên tỉnh/thành phố (34 tỉnh)
    
    @Column(name = "ward_code_34", length = 10)
    private String wardCode34; // Mã phường/xã (34 tỉnh)
    
    @Column(name = "ward_name_34", length = 100)
    private String wardName34; // Tên phường/xã (34 tỉnh)

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
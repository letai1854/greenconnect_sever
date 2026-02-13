package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_token_expiry", columnList = "expiry_date"),
    @Index(name = "idx_refresh_token_user_expiry", columnList = "user_id, expiry_date"),
    // ✅ NEW: Device tracking optimization
    @Index(name = "idx_refresh_token_user_device", columnList = "user_id, device_info")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY) // ⭐ ĐỔI từ @OneToOne thành @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // ⭐ XÓA unique = true
    private User user;
    
    @Column(name = "token_hash", nullable = false, unique = true,length = 4096)
    private String tokenHash;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(name = "device_info") // ⭐ THÊM device tracking
    private String deviceInfo;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.greenconnect.greenconnect_api.enums.DeviceType;

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
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity cho FCM (Firebase Cloud Messaging) Tokens
 * Quản lý push notification tokens của users trên các thiết bị khác nhau
 */
@Entity
@Table(name = "fcm_tokens", indexes = {
    @Index(name = "idx_fcm_token", columnList = "token", unique = true),
    @Index(name = "idx_fcm_user", columnList = "user_id"),
    @Index(name = "idx_fcm_user_device", columnList = "user_id, device_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * FCM Token từ Firebase (Unique constraint)
     * Token này được tạo khi user đăng nhập và đăng ký nhận notification
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;
    
    /**
     * Loại thiết bị: ANDROID, IOS, WEB
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;
    
    /**
     * Thời gian cập nhật cuối cùng
     * Tự động update mỗi khi save/update entity
     */
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    /**
     * Trạng thái active của token
     * false = token đã logout hoặc không còn valid
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}

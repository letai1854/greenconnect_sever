package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lưu trữ thông tin thiết bị của người dùng cho push notifications.
 * Mỗi user có thể có nhiều thiết bị với các FCM token khác nhau.
 */
@Entity
@Table(name = "user_devices", indexes = {
    // ✅ Critical: Lookup device by FCM token (send notification)
    @jakarta.persistence.Index(name = "idx_device_fcm_token", columnList = "user_id"),
    // ✅ Important: Get all devices of user (broadcast notification)
    @jakarta.persistence.Index(name = "idx_device_user_id", columnList = "user_id"),
    // ✅ Useful: Filter by device type
    @jakarta.persistence.Index(name = "idx_device_type", columnList = "device_type"),
    // ✅ NEW: User + device type (targeted notification)
    @jakarta.persistence.Index(name = "idx_device_user_type", columnList = "user_id, device_type"),
    // ✅ NEW: Recently active devices
    @jakarta.persistence.Index(name = "idx_device_user_updated", columnList = "user_id, updated_at"),
    // ✅ NEW: Cleanup old/inactive devices
    @jakarta.persistence.Index(name = "idx_device_updated", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "fcm_token", columnDefinition = "TEXT", unique = true)
    private String fcmToken; // Firebase Cloud Messaging token
    
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType; // ANDROID, IOS, WEB
    
    @Column(name = "device_name", length = 255)
    private String deviceName; // VD: "iPhone 14 Pro", "Samsung Galaxy S23"
    
    @Column(name = "os_version", length = 50)
    private String osVersion; // VD: "17.0", "14.0"
    
    @Column(name = "app_version", length = 50)
    private String appVersion; // VD: "1.0.0", "2.1.5"
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true; // Deactivate nếu user unsubscribe
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

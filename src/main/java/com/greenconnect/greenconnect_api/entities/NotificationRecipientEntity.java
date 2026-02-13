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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bảng trung gian lưu trữ người nhận và trạng thái đọc của từng thông báo
 * Cho phép 1 notification gửi đến nhiều user, mỗi user có trạng thái đọc riêng
 */
@Entity
@Table(name = "notification_recipients", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_user", columnNames = {"notification_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_notif_recipient_user", columnList = "user_id"),
        @Index(name = "idx_notif_recipient_notification", columnList = "notification_id"),
        @Index(name = "idx_notif_recipient_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notif_recipient_created", columnList = "created_at DESC")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipientEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Thông báo (1 notification có nhiều recipients)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;
    
    /**
     * User nhận thông báo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Trạng thái đã đọc của user này
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    /**
     * Thời điểm user đọc thông báo (null nếu chưa đọc)
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    /**
     * Thời điểm tạo recipient (khi gửi thông báo cho user này)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

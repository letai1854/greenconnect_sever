package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity lưu trữ activation token cho user mới được admin mời.
 * <p>Hoàn toàn tách biệt với OtpCode (dùng cho forgot password).</p>
 */
@Entity
@Table(name = "invitation_tokens", indexes = {
    @jakarta.persistence.Index(name = "idx_invitation_email", columnList = "email"),
    @jakarta.persistence.Index(name = "idx_invitation_token_hash", columnList = "token_hash"),
    @jakarta.persistence.Index(name = "idx_invitation_expiry", columnList = "expiry_date"),
    @jakarta.persistence.Index(name = "idx_invitation_status", columnList = "is_used, expiry_date"),
    // ✅ NEW: Find by invited admin
    @jakarta.persistence.Index(name = "idx_invitation_invited_by", columnList = "invited_by_id"),
    // ✅ NEW: Activated user lookup
    @jakarta.persistence.Index(name = "idx_invitation_activated_user", columnList = "activated_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Email của user được mời (chưa có account trong users table)
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    /**
     * Họ tên user (lưu sẵn để gửi email)
     */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
    
    /**
     * Token đã hash (bảo mật)
     * Token gốc sẽ gửi qua email, không lưu plaintext
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;
    
    /**
     * Thời gian hết hạn (24h kể từ khi tạo)
     */
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    /**
     * Đánh dấu token đã được sử dụng (activate thành công)
     */
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;
    
    /**
     * UUID của admin tạo invitation
     */
    @Column(name = "invited_by_id", nullable = false)
    private UUID invitedByAdminId;
    
    /**
     * Thời gian tạo token
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Thời gian user activate (null nếu chưa activate)
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
    
    /**
     * User ID sau khi activate thành công (foreign key)
     */
    @Column(name = "activated_user_id")
    private UUID activatedUserId;
}

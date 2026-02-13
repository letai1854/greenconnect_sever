package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.greenconnect.greenconnect_api.enums.PaymentMethod;
import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

@Entity
@Table(name = "users", indexes = {
    // ✅ Critical: Authentication lookup (every login)
    @Index(name = "idx_user_email", columnList = "email"),
    // ✅ Important: Admin user management  
    @Index(name = "idx_user_status", columnList = "status"),
    // ✅ Useful: Search users by phone
    @Index(name = "idx_user_phone", columnList = "phone_number"),
    // ✅ NEW: Firebase login optimization
    @Index(name = "idx_user_email_provider", columnList = "email, provider"),
    // ✅ NEW: Admin filtering optimization  
    @Index(name = "idx_user_status_created", columnList = "status, created_at"),
    // ✅ NEW: User search optimization
    @Index(name = "idx_user_fullname", columnList = "full_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_code", unique = true, columnDefinition = "BIGINT")
    private Long userCode;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;
    
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Builder.Default
    private Provider provider = Provider.LOCAL;
    
    @Column(name = "provider_id", length = 255)
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "loyalty_points", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal loyaltyPoints = BigDecimal.ZERO;
    
    @Column(name = "tong_tien", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPaymentAmount = BigDecimal.ZERO; // Tổng tiền thanh toán tất cả đơn hàng
    
    @Column(name = "name_account_bank", length = 255)
    private String nameAccountBank; // Tên của tài khoản ngân hàng
    
    @Column(name = "name_bank", length = 255)
    private String nameBank; // Tên ngân hàng
    
    @Column(name = "account_number_bank")
    private Long accountNumberBank; // Số tài khoản ngân hàng
    
    @Column(name = "name_bank_code", length = 255)
    private String nameBankCode; // Mã ngân hàng
    
    // ========== PAYMENT PREFERENCES ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_payment_method", nullable = false)
    @Builder.Default
    private PaymentMethod preferredPaymentMethod = PaymentMethod.COD;
    
    // ========== 2FA (TOTP) FIELDS ==========
    
    @Column(name = "secret_key_2fa", length = 255)
    private String secretKey2FA; // Secret key cho TOTP (Google Authenticator)
    
    @Column(name = "totp_enabled", nullable = false)
    @Builder.Default
    private Boolean totpEnabled = false; // Trạng thái kích hoạt 2FA
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Address> addresses;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>(); // ⭐ ĐỔI từ RefreshToken thành List<RefreshToken>
    
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OtpCode> otpCodes;
    
    /**
     * FCM Tokens cho push notifications trên nhiều thiết bị
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcmToken> fcmTokens = new ArrayList<>();
    
    /**
     * Multiple roles cho user thông qua UserRole junction table.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();
    
    // Helper methods for roles
    public List<RefreshToken> getActiveRefreshTokens() {
        return refreshTokens.stream()
            .filter(token -> token.getExpiryDate().isAfter(LocalDateTime.now()))
            .collect(Collectors.toList());
    }
    /**
     * Lấy tất cả active roles của user.
     */
    public Set<Role> getActiveRoles() {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
    }
    
    /**
     * Lấy primary role của user (dùng cho JWT và display).
     */
    public Role getPrimaryRole() {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(UserRole::getRole)
                .findFirst()
                .orElse(Role.CUSTOMER); // Default fallback
    }
    
    /**
     * Kiểm tra user có role cụ thể không.
     */
    public boolean hasRole(Role role) {
        return getActiveRoles().contains(role);
    }
    
    /**
     * Kiểm tra user có bất kỳ role nào trong danh sách không.
     */
    public boolean hasAnyRole(Role... roles) {
        Set<Role> userRoles = getActiveRoles();
        for (Role role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Thêm role mới cho user.
     */
    public void addRole(Role role, boolean isPrimary) {
        UserRole userRole = UserRole.builder()
                .user(this)
                .role(role)
                .active(true)
                .build();
        
        userRoles.add(userRole);
    }
    
    /**
     * Xóa role khỏi user.
     */
    public void removeRole(Role role) {
        userRoles.removeIf(ur -> ur.getRole() == role);
    }
}
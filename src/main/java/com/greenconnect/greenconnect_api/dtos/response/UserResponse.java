package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    
    // ========== BASIC USER INFO ==========
    UUID id;
    String email;
    String fullName;
    String phoneNumber;
    String avatarUrl;
    
    // ========== ACCOUNT INFO ==========
    Provider provider;          // LOCAL, GOOGLE, FACEBOOK, APPLE
    Set<Role> roles;            // All active roles assigned to the user
    Role primaryRole;           // Primary role of the user (for JWT claims)
    UserStatus status;          // ACTIVE, INACTIVE, BANNED
    Boolean totpEnabled;        // 🔐 2FA/TOTP enabled status (for admin view)
    
    // ========== BUSINESS INFO ==========
    Long userCode;              // Mã user (unique)
    BigDecimal loyaltyPoints;   // Điểm loyalty
    
    // ========== FINANCIAL INFO (Banking Details) ==========
    BigDecimal totalPaymentAmount;  // Tổng tiền thanh toán tất cả đơn hàng
    String nameAccountBank;         // Tên chủ tài khoản ngân hàng
    String nameBank;                // Tên ngân hàng
    Long accountNumberBank;         // Số tài khoản ngân hàng
    String nameBankCode;            // Mã ngân hàng (SWIFT code)
    
    // ========== PAYMENT PREFERENCES ==========
    com.greenconnect.greenconnect_api.enums.PaymentMethod preferredPaymentMethod; // Phương thức thanh toán ưa tiên (COD, VNPAY, MOMO, VIETQR)
    
    // ========== DEFAULT ADDRESS ==========
    AddressResponse defaultAddress; // Địa chỉ mặc định của user (isDefault=true)
    
    // ========== TIMESTAMPS ==========
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    // ========== NOTE - FIELDS NOT INCLUDED ==========
    // ❌ Excluded: passwordHash (security sensitive)
    // ❌ Excluded: providerId (security sensitive)
    // ❌ Excluded: refreshTokens (sensitive, tracked separately)
    // ❌ Excluded: otpCodes (temporary, sensitive)
    // ❌ Excluded: userRoles (entity list, use roles Set instead)
    // ❌ Excluded: addresses (fetch separately)
    // ❌ Excluded: FCM tokens (tracked in UserDevice table)
}
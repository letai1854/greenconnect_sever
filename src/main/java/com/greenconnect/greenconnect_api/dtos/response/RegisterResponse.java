package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * Response cho endpoint đăng ký (/users/register)
 * <p>Khác với LoginResponse:</p>
 * <ul>
 *   <li>✅ Trả về access token + refresh token (giống login)</li>
 *   <li>✅ Trả về user info đầy đủ</li>
 *   <li>✅ Thêm trường `status` để biểu thị trạng thái đăng ký (SUCCESS, PENDING_VERIFICATION, etc.)</li>
 *   <li>✅ Thêm trường `statusCode` (200 = thành công, 201 = created, etc.)</li>
 *   <li>✅ Thêm trường `isNewUser` để frontend biết đây là user mới tạo</li>
 *   <li>✅ Thêm trường `requiresVerification` cho email verification flow</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterResponse {
    
    // ========== STATUS INFORMATION ==========
    
    String status;                    // ✅ "SUCCESS" | "PENDING_VERIFICATION" | "ERROR"
    Integer statusCode;               // ✅ HTTP status code (200, 201, 400, 500, etc.)
    String message;                   // Chi tiết thông báo (VD: "Đăng ký thành công")
    
    // ========== USER IDENTIFICATION ==========
    
    UUID userId;                      // User ID mới tạo
    
    // ========== ACCESS TOKEN INFORMATION ==========
    
    String accessToken;               // JWT access token (15 phút)
    Long accessTokenExpiresIn;        // Thời gian hết hạn access token (seconds)
    
    // ========== REFRESH TOKEN INFORMATION ==========
    
    String refreshToken;              // Refresh token (30 ngày)
    Long refreshTokenExpiresIn;       // Thời gian hết hạn refresh token (seconds)
    
    // ========== TOKEN METADATA ==========
    
    String tokenType;                 // "Bearer"
    LocalDateTime issuedAt;           // Thời gian tạo token
    
    // ========== USER INFORMATION ==========
    
    UserResponse user;                // Thông tin user mới tạo (với tất cả fields)
    
    // ========== REGISTRATION METADATA ==========
    
    Boolean isNewUser;                // ✅ Luôn true cho /register endpoint (user mới)
    Boolean requiresEmailVerification; // Có cần verify email không (tuỳ config)
}

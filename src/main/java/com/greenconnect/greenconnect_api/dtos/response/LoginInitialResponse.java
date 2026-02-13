package com.greenconnect.greenconnect_api.dtos.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * Response DTO cho bước đầu tiên của đăng nhập (trước khi verify OTP)
 * <p>Không chứa access token và refresh token</p>
 * <p>Chỉ thông báo cho client biết cần quét QR hay nhập OTP</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginInitialResponse {
    
    UUID userId;                    // User ID để track
    
    Boolean is2FAActivated;         // true = đã kích hoạt 2FA, false = chưa kích hoạt
    
    String qrCodeUrl;               // QR code URL (nếu chưa kích hoạt 2FA)
    
    String secretKey;               // Secret key cho manual entry (nếu chưa kích hoạt 2FA)
    
    String message;                 // Thông báo cho client
    
    UserResponse user;              // Thông tin user (không có sensitive data)
}

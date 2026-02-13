package com.greenconnect.greenconnect_api.dtos.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO cho việc xác thực OTP (TOTP)
 * <p>Client gửi mã 6 số từ Google Authenticator để hoàn tất đăng nhập</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyOtpRequest {
    
    @NotNull(message = "User ID không được để trống")
    UUID userId;
    
    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP phải là 6 chữ số")
    String otpCode;
    
    // Device info cho tracking
    String deviceInfo;
    
    String fcmToken;
    
    String deviceType;
}

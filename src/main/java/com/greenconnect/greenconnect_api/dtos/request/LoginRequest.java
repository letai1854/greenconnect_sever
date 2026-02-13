package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class LoginRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    String email;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
    String password;
    
    // ⭐ Device tracking cho multi-device support
    @Size(max = 500, message = "Device info không được vượt quá 500 ký tự")
    String deviceInfo; // Optional: "Web Chrome 119.0.0.0 - Windows 11" hoặc "Mobile Android 13"
    
    // ⭐ FCM Token cho push notifications
    @Size(max = 4096, message = "FCM token không được vượt quá 4096 ký tự")
    String fcmToken; // Optional: Firebase Cloud Messaging token
    
    // ⭐ Device type
    String deviceType; // Optional: ANDROID, IOS, WEB (enum string)
}
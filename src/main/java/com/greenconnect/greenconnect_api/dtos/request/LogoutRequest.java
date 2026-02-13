package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
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
public class LogoutRequest {
    
    @NotBlank(message = "Refresh token không được để trống")
    String refreshToken;
    
    // Optional: FCM token để xóa khi logout
    String fcmToken;
    
    // Optional: Có thể thêm device info để logging
    String deviceInfo;
}
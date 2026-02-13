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

/**
 * Firebase Login Request DTO
 * <p>Chứa thông tin thiết yếu để đăng nhập qua Firebase (Google)</p>
 * <p>Để phân biệt với RegisterFirebaseRequest (đăng ký mới):</p>
 * <ul>
 *   <li>fullName: optional (nếu có thì update profile)</li>
 *   <li>photoURL: optional (nếu có thì update profile)</li>
 *   <li>deviceInfo, fcmToken, deviceType: để track device</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginFirebaseRequest {
    
    /**
     * Email từ Firebase
     */
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    String email; 
    
    /**
     * Firebase User ID hoặc Google User ID (dùng để identify user)
     */
    @NotBlank(message = "Provider ID không được để trống") 
    String providerId;
    
    /**
     * Tên người dùng từ Firebase profile (optional, để update profile nếu cần)
     */
    @Size(min = 2, max = 255, message = "Tên người dùng phải từ 2-255 ký tự")
    String fullName;
    
    /**
     * Avatar URL từ Firebase (optional, để update profile nếu cần)
     */
    String photoURL;
    
    // ⭐ Device tracking cho multi-device support
    @Size(max = 500, message = "Device info không được vượt quá 500 ký tự")
    String deviceInfo; // Optional: "iPhone 12 Pro/iOS 17.0" hoặc "Samsung Galaxy S21/Android 13"
    
    // ⭐ FCM Token cho push notifications
    @Size(max = 4096, message = "FCM token không được vượt quá 4096 ký tự")
    String fcmToken; // Optional: Firebase Cloud Messaging token
    
    // ⭐ Device type
    String deviceType; // Optional: ANDROID, IOS, WEB (enum string)
}

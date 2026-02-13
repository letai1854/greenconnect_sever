package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;

/**
 * Response DTO cho thông tin đầy đủ của user (giống LoginResponse nhưng không có tokens).
 * <p>Sử dụng cho các endpoint lấy thông tin user mà không cần authentication tokens.</p>
 * <p>Bao gồm: user info, addresses, favorites - giống như response sau khi login nhưng không có accessToken/refreshToken</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    
    // ========== USER IDENTIFICATION ==========
    
    UUID userId;                   // User ID (để client track)
    
    // ========== USER INFORMATION ==========
    
    UserResponse user;             // Thông tin user đầy đủ (với tất cả fields)
    
    // ========== ADDRESS INFORMATION ==========
    
    List<AddressRespone> addresses; // Danh sách địa chỉ của user
    
    // ========== FAVORITE INFORMATION ==========
    
    List<FavoriteResponse> favorites; // Danh sách sản phẩm yêu thích của user
    
    // ========== METADATA ==========
    
    LocalDateTime retrievedAt;     // Thời gian lấy thông tin
}

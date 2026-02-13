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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginResponse {
    
    // ========== USER IDENTIFICATION ==========
    
    UUID userId;                   // User ID (để client track)
    
    // ========== ACCESS TOKEN INFORMATION ==========
    
    String accessToken;            // JWT access token (15 phút)
    Long accessTokenExpiresIn;     // Thời gian hết hạn access token (seconds)
    
    // ========== REFRESH TOKEN INFORMATION ==========
    
    String refreshToken;           // Refresh token (30 ngày cho regular, 7 ngày cho Firebase)
    Long refreshTokenExpiresIn;    // Thời gian hết hạn refresh token (seconds)
    
    // ========== TOKEN METADATA ==========
    
    String tokenType;              // "Bearer"
    LocalDateTime issuedAt;        // Thời gian tạo token
    
    // ========== USER INFORMATION ==========
    
    UserResponse user;             // Thông tin user đã đăng nhập (với tất cả fields)
    
    // ========== ADDRESS INFORMATION ==========
    
    List<AddressRespone> addresses; // Danh sách địa chỉ của user
    
    // ========== FAVORITE INFORMATION ==========
    
    List<FavoriteResponse> favorites; // Danh sách sản phẩm yêu thích của user
}
package com.greenconnect.greenconnect_api.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.services.UserService;
import com.greenconnect.greenconnect_api.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller xử lý profile của user hiện tại
 * Dùng để client lấy thông tin mới nhất sau khi nhận WebSocket notification
 * 
 * Use case:
 * 1. Admin thay đổi role của user
 * 2. Backend gửi WebSocket notification đến user
 * 3. Flutter app nhận notification
 * 4. Flutter app gọi API này để lấy thông tin mới nhất (role updated)
 * 5. Flutter app cập nhật UI dựa trên role mới
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    
    /**
     * Lấy thông tin profile hiện tại của user (từ DB, KHÔNG cache)
     * Client gọi API này khi nhận được notification về role change
     * 
     * Flow:
     * - Client nhận WebSocket notification với event="ROLE_UPDATED"
     * - Client gọi API này để lấy UserResponse mới nhất từ database
     * - Client cập nhật AuthState với roles mới
     * - UI tự động rebuild theo BlocBuilder
     * 
     * @return ApiResponse<UserResponse> chứa thông tin user với roles mới nhất
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> getCurrentUserProfile() {
        String currentEmail = SecurityUtils.getCurrentUserEmail();
        
        log.info("🔄 [REFRESH PROFILE] User '{}' đang lấy lại thông tin profile mới nhất", currentEmail);
        
        // Lấy thông tin user mới nhất từ database (có roles đã được cập nhật)
        UserResponse userResponse = userService.getUserByEmail(currentEmail);
        
        log.info("✅ [REFRESH PROFILE] Trả về profile với roles mới: {}", userResponse.getRoles());
        
        return ResponseUtil.success(userResponse, "Lấy thông tin profile thành công");
    }
}

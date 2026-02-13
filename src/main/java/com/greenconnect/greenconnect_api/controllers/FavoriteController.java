package com.greenconnect.greenconnect_api.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.services.FavoriteService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;
import com.greenconnect.greenconnect_api.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FavoriteController - Quản lý sản phẩm yêu thích
 * Giới hạn tối đa 20 sản phẩm yêu thích cho mỗi user
 */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    /**
     * Thêm sản phẩm vào danh sách yêu thích
     * User ID được lấy từ JWT token (không cần truyền vào)
     */
    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addFavorite(@PathVariable UUID productId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} thêm sản phẩm {} vào yêu thích", userId, productId);
        favoriteService.addFavorite(userId, productId);
        
        // 🔄 Track bookmark in Recombee
        if (recombeeSyncService != null) {
            try {
                recombeeSyncService.trackBookmark(userId, productId);
                log.info("✅ [RECOMBEE TRACKING] Bookmark tracked → user={}, product={}", userId, productId);
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track bookmark: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ [RECOMBEE] Service not available - Bookmark tracking skipped");
        }
        
        return ResponseEntity.ok(ResponseUtil.success(null, "Đã thêm vào danh sách yêu thích"));
    }

    /**
     * Xóa sản phẩm khỏi danh sách yêu thích
     * User ID được lấy từ JWT token (không cần truyền vào)
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@PathVariable UUID productId) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} xóa sản phẩm {} khỏi yêu thích", userId, productId);
        favoriteService.removeFavorite(userId, productId);
        return ResponseEntity.ok(ResponseUtil.success(null, "Đã xóa khỏi danh sách yêu thích"));
    }

    /**
     * Lấy danh sách sản phẩm yêu thích của user hiện tại
     * User ID được lấy từ JWT token (không cần truyền vào)
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> listFavorites(Pageable pageable) {
        
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("User {} lấy danh sách yêu thích", userId);
        Page<ProductResponse> favorites = favoriteService.listFavorites(userId, pageable);
        return ResponseEntity.ok(ResponseUtil.success(favorites, "Lấy danh sách yêu thích thành công"));
    }
}

package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.AddToCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.BatchUpdateCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCartItemRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.BatchUpdateCartResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.CartService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cart Controller - Quản lý giỏ hàng
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    
    private final CartService cartService;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    /**
     * Thêm sản phẩm vào giỏ hàng
     * ⚠️ Deprecated: Khuyến nghị dùng /cart/add-or-update để tránh duplicate tracking
     */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        UUID userId = getCurrentUserId();
        log.info("Thêm sản phẩm vào giỏ hàng - User: {}, Variant: {}", userId, request.getProductVariantId());
        
        // Kiểm tra sản phẩm đã có trong cart chưa để tránh duplicate tracking
        var existingItem = cartService.getCartItemByUserAndVariant(userId, request.getProductVariantId());
        boolean isNewItem = existingItem.isEmpty();
        
        CartItemResponse response = cartService.addToCart(userId, request);
        
        // 🔄 Track cart addition in Recombee (chỉ khi thêm mới)
        if (isNewItem && recombeeSyncService != null && response != null && response.getProductVariant() != null) {
            try {
                UUID productId = response.getProductVariant().getProduct().getId();
                recombeeSyncService.trackCartAddition(userId, productId, request.getQuantity());
                log.info("✅ [RECOMBEE TRACKING] Cart addition tracked → user={}, product={}, amount={}", 
                    userId, productId, request.getQuantity());
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track cart addition: {}", e.getMessage());
            }
        } else if (!isNewItem) {
            log.debug("ℹ️ [RECOMBEE] Item already in cart - Tracking skipped to avoid duplicate");
        } else if (recombeeSyncService == null) {
            log.warn("⚠️ [RECOMBEE] Service not available - Cart tracking skipped");
        }
        
        ApiResponse<CartItemResponse> apiResponse = ResponseUtil.success(
            response, 
            "Thêm sản phẩm vào giỏ hàng thành công"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
    
    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItem(@Valid @RequestBody UpdateCartItemRequest request) {
        UUID userId = getCurrentUserId();
        log.info("Cập nhật cart item - User: {}, CartItem: {}", userId, request.getCartItemId());
        
        CartItemResponse response = cartService.updateCartItem(userId, request);
        
        if (response == null) {
            // Item đã bị xóa do quantity = 0
            ApiResponse<CartItemResponse> apiResponse = ResponseUtil.success(
                null, 
                "Đã xóa sản phẩm khỏi giỏ hàng"
            );
            return ResponseEntity.ok(apiResponse);
        }
        
        ApiResponse<CartItemResponse> apiResponse = ResponseUtil.success(
            response, 
            "Cập nhật giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy tất cả sản phẩm trong giỏ hàng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        UUID userId = getCurrentUserId();
        log.info("Lấy giỏ hàng của user: {}", userId);
        
        CartResponse response = cartService.getCartByUserId(userId);
        ApiResponse<CartResponse> apiResponse = ResponseUtil.success(
            response, 
            "Lấy giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     */
    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(@PathVariable UUID cartItemId) {
        UUID userId = getCurrentUserId();
        log.info("Xóa cart item - User: {}, CartItem: {}", userId, cartItemId);
        
        cartService.removeCartItem(userId, cartItemId);
        ApiResponse<Void> apiResponse = ResponseUtil.success(
            null, 
            "Xóa sản phẩm khỏi giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Xóa tất cả sản phẩm trong giỏ hàng
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        UUID userId = getCurrentUserId();
        log.info("Xóa tất cả cart items của user: {}", userId);
        
        cartService.clearCart(userId);
        ApiResponse<Void> apiResponse = ResponseUtil.success(
            null, 
            "Xóa tất cả sản phẩm trong giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Đếm số lượng items trong giỏ hàng
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCartItemCount() {
        UUID userId = getCurrentUserId();
        log.info("Đếm số lượng cart items của user: {}", userId);
        
        long count = cartService.getCartItemCount(userId);
        ApiResponse<Long> apiResponse = ResponseUtil.success(
            count, 
            "Lấy số lượng sản phẩm trong giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Tính tổng số lượng sản phẩm trong giỏ hàng (for badge)
     */
    @GetMapping("/total-quantity")
    public ResponseEntity<ApiResponse<Integer>> getTotalQuantity() {
        UUID userId = getCurrentUserId();
        log.info("Tính tổng quantity của user: {}", userId);
        
        Integer totalQuantity = cartService.getTotalQuantity(userId);
        ApiResponse<Integer> apiResponse = ResponseUtil.success(
            totalQuantity, 
            "Lấy tổng số lượng sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Thêm hoặc cập nhật sản phẩm trong giỏ hàng
     * Nếu sản phẩm chưa có -> thêm mới
     * Nếu sản phẩm đã có -> cập nhật quantity (cộng thêm)
     */
    @PostMapping("/add-or-update")
    public ResponseEntity<ApiResponse<CartItemResponse>> addOrUpdateCart(@Valid @RequestBody AddToCartRequest request) {
        UUID userId = getCurrentUserId();
        log.info("Thêm hoặc cập nhật cart - User: {}, Variant: {}, Quantity: {}", 
                userId, request.getProductVariantId(), request.getQuantity());
        
        CartItemResponse response;
        boolean isUpdate = false;
        
        // Kiểm tra sản phẩm có trong cart không
        var existingItem = cartService.getCartItemByUserAndVariant(userId, request.getProductVariantId());
        
        if (existingItem.isEmpty()) {
            // Chưa có trong cart -> thêm mới
            response = cartService.addToCart(userId, request);
            log.info("Đã thêm sản phẩm mới vào cart");
        } else {
            // Đã có trong cart -> lấy quantity hiện tại + quantity mới rồi update
            var cartItem = existingItem.get();
            int currentQuantity = cartItem.getQuantity();
            int newTotalQuantity = currentQuantity + request.getQuantity();
            
            UpdateCartItemRequest updateRequest = UpdateCartItemRequest.builder()
                .cartItemId(cartItem.getId())
                .quantity(newTotalQuantity)
                .build();
            
            response = cartService.updateCartItem(userId, updateRequest);
            isUpdate = true;
            log.info("Đã cập nhật quantity: {} + {} = {}", currentQuantity, request.getQuantity(), newTotalQuantity);
        }
        
        // 🔄 Track cart addition in Recombee (chỉ khi thêm mới, không track update quantity)
        if (!isUpdate && recombeeSyncService != null && response != null && response.getProductVariant() != null) {
            try {
                UUID productId = response.getProductVariant().getProduct().getId();
                recombeeSyncService.trackCartAddition(userId, productId, request.getQuantity());
                log.info("✅ [RECOMBEE TRACKING] Cart addition tracked → user={}, product={}, amount={}", 
                    userId, productId, request.getQuantity());
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track cart addition: {}", e.getMessage());
            }
        } else if (recombeeSyncService == null) {
            log.debug("ℹ️ [RECOMBEE] Service not available - Cart tracking skipped");
        }
        
        ApiResponse<CartItemResponse> apiResponse = ResponseUtil.success(
            response,
            isUpdate ? "Cập nhật số lượng sản phẩm thành công" : "Thêm sản phẩm vào giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Xóa nhiều sản phẩm khỏi giỏ hàng theo list ID
     */
    @DeleteMapping("/items/batch")
    public ResponseEntity<ApiResponse<Void>> removeCartItems(@Valid @RequestBody List<UUID> cartItemIds) {
        UUID userId = getCurrentUserId();
        log.info("Xóa nhiều cart items - User: {}, Items: {}", userId, cartItemIds);
        
        for (UUID cartItemId : cartItemIds) {
            cartService.removeCartItem(userId, cartItemId);
        }
        
        ApiResponse<Void> apiResponse = ResponseUtil.success(
            null,
            "Xóa các sản phẩm khỏi giỏ hàng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Cập nhật nhiều sản phẩm trong giỏ hàng cùng lúc (Batch Update)
     * 
     * 🎯 Use case chính: Optimistic UI + Debounce pattern
     * - Khi người dùng sửa nhiều sản phẩm trong giỏ hàng rồi thoát màn hình
     * - Flutter gom tất cả thay đổi lại và gọi 1 API duy nhất
     * - Giảm tải cho server, tránh race condition
     * 
     * ✨ Tính năng đặc biệt:
     * - Tự động điều chỉnh quantity nếu vượt quá stock (Optimistic UI rollback)
     * - Trả về kết quả chi tiết cho từng item
     * - Hỗ trợ xóa item (quantity = 0)
     */
    @PutMapping("/update-batch")
    public ResponseEntity<ApiResponse<BatchUpdateCartResponse>> batchUpdateCart(
            @Valid @RequestBody BatchUpdateCartRequest request) {
        UUID userId = getCurrentUserId();
        log.info("Batch update cart - User: {}, Items: {}", userId, request.getItems().size());
        
        BatchUpdateCartResponse response = cartService.batchUpdateCartItems(userId, request);
        
        String message = String.format(
            "Batch update hoàn tất: %d thành công, %d xóa, %d thất bại",
            response.getSuccessCount(),
            response.getDeletedCount(),
            response.getFailedCount()
        );
        
        ApiResponse<BatchUpdateCartResponse> apiResponse = ResponseUtil.success(response, message);
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy user ID từ Security Context
     */
    private UUID getCurrentUserId() {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userPrincipal.getUserId();
    }
}
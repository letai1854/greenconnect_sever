package com.greenconnect.greenconnect_api.services;

import java.util.Optional;
import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.AddToCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.BatchUpdateCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCartItemRequest;
import com.greenconnect.greenconnect_api.dtos.response.BatchUpdateCartResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartResponse;
import com.greenconnect.greenconnect_api.entities.CartItem;

/**
 * Service interface cho Cart operations
 */
public interface CartService {
    
    /**
     * Thêm sản phẩm vào giỏ hàng
     * Nếu sản phẩm đã có trong giỏ hàng thì tăng số lượng
     */
    CartItemResponse addToCart(UUID userId, AddToCartRequest request);
    
    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * Nếu quantity = 0 thì xóa item khỏi giỏ hàng
     */
    CartItemResponse updateCartItem(UUID userId, UpdateCartItemRequest request);
    
    /**
     * Lấy tất cả sản phẩm trong giỏ hàng của user
     */
    CartResponse getCartByUserId(UUID userId);
    
    /**
     * Xóa một item khỏi giỏ hàng
     */
    void removeCartItem(UUID userId, UUID cartItemId);
    
    /**
     * Xóa tất cả items trong giỏ hàng của user
     */
    void clearCart(UUID userId);
    
    /**
     * Đếm số lượng items trong giỏ hàng
     */
    long getCartItemCount(UUID userId);
    
    /**
     * Tính tổng số lượng sản phẩm trong giỏ hàng
     */
    Integer getTotalQuantity(UUID userId);
    
    /**
     * Kiểm tra xem sản phẩm có trong giỏ hàng chưa
     */
    boolean isItemInCart(UUID userId, UUID productVariantId);
    
    /**
     * Lấy CartItem theo userId và productVariantId
     */
    Optional<CartItem> getCartItemByUserAndVariant(UUID userId, UUID productVariantId);
    
    /**
     * Cập nhật nhiều sản phẩm trong giỏ hàng cùng lúc (Batch Update)
     * Dùng cho Optimistic UI + Debounce pattern
     * @param userId ID người dùng
     * @param request Danh sách các item cần cập nhật
     * @return Kết quả cập nhật cho từng item
     */
    BatchUpdateCartResponse batchUpdateCartItems(UUID userId, BatchUpdateCartRequest request);
}
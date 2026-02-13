package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.CartItem;

/**
 * Repository interface cho CartItem
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    
    /**
     * Tìm cart item của user với product variant cụ thể
     * Dùng để check duplicate và merge quantity
     */
    Optional<CartItem> findByUserIdAndProductVariantId(UUID userId, UUID productVariantId);
    
    /**
     * Lấy tất cả cart items của user với eager loading.
     * Bao gồm ProductVariant -> Product -> ProductImages để tránh N+1 queries.
     * Sắp xếp theo ngày thêm mới nhất.
     * <p>Index sử dụng: idx_cart_user_added (user_id, added_date), 
     * idx_product_images_product (product_id)</p>
     */
    @Query("SELECT DISTINCT c FROM CartItem c " +
           "LEFT JOIN FETCH c.productVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.productImages " +
           "WHERE c.user.id = :userId " +
           "ORDER BY c.addedDate DESC")
    List<CartItem> findByUserIdWithDetails(@Param("userId") UUID userId);
    
    /**
     * Lấy cart items với product variant active.
     * Bao gồm ProductVariant -> Product -> ProductImages để tránh N+1 queries.
     * <p>Index sử dụng: idx_cart_user_added (user_id, added_date),
     * idx_variant_product_active (product_id, is_active),
     * idx_product_images_product (product_id)</p>
     */
    @Query("SELECT DISTINCT c FROM CartItem c " +
           "LEFT JOIN FETCH c.productVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.productImages " +
           "WHERE c.user.id = :userId " +
           "AND pv.isActive = true " +
           "AND p.isActive = true " +
           "ORDER BY c.addedDate DESC")
    List<CartItem> findActiveByUserIdWithDetails(@Param("userId") UUID userId);
    
    /**
     * Đếm số lượng items trong cart của user
     */
    long countByUserId(UUID userId);
    
    /**
     * Xóa tất cả cart items của user
     */
    void deleteByUserId(UUID userId);
    
    /**
     * Kiểm tra cart item có thuộc về user không
     */
    boolean existsByIdAndUserId(UUID cartItemId, UUID userId);
    
    /**
     * Tính tổng quantity của tất cả items trong cart
     * Sử dụng index: idx_cart_user_count
     */
    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItem c WHERE c.user.id = :userId")
    Integer getTotalQuantityByUserId(@Param("userId") UUID userId);
    
    /**
     * Lấy cart items được cập nhật gần đây (cho analytics)
     * Sử dụng index: idx_cart_updated
     */
    @Query("SELECT c FROM CartItem c WHERE c.updatedDate >= :since ORDER BY c.updatedDate DESC")
    List<CartItem> findRecentlyUpdated(@Param("since") LocalDateTime since);
    
    /**
     * Xóa cart items cũ (cleanup job)
     * Sử dụng index: idx_cart_updated
     */
    @Query("DELETE FROM CartItem c WHERE c.updatedDate < :before")
    void deleteOldCartItems(@Param("before") LocalDateTime before);
}
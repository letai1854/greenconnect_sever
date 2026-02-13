package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cart_items", indexes = {
    // ✅ Critical: Load giỏ hàng của user (every page load) 
    @Index(name = "idx_cart_user_added", columnList = "user_id, addedDate DESC"),
    // ✅ Important: Check duplicate item trong cart (addToCart performance)
    @Index(name = "idx_cart_user_variant", columnList = "user_id, productVariant_id"),
    // ✅ Essential: Quản lý stock theo variant (inventory management)
    @Index(name = "idx_cart_variant", columnList = "productVariant_id"),
    // ✅ Performance: Count cart items by user (cart badge counter)
    @Index(name = "idx_cart_user_count", columnList = "user_id"),
    // ✅ Security: Verify cart item ownership (updateCartItem, removeCartItem)
    @Index(name = "idx_cart_id_user", columnList = "id, user_id"),
    // ✅ Admin: Analytics và cleanup operations
    @Index(name = "idx_cart_updated", columnList = "updatedDate"),
    // ✅ Composite: User active carts with recent activity
    @Index(name = "idx_cart_user_updated", columnList = "user_id, updatedDate DESC"),
    // ✅ NEW: Join optimization (cart checkout - bulk join with variant/product)
    @Index(name = "idx_cart_user_variant_added", columnList = "user_id, productVariant_id, addedDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productVariant_id", nullable = false)
    private ProductVariant productVariant;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @CreationTimestamp
    @Column(name = "addedDate")
    private LocalDateTime addedDate;
    
    @UpdateTimestamp
    @Column(name = "updatedDate")
    private LocalDateTime updatedDate;
}
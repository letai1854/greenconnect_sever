package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
    // ✅ Critical: Lọc sản phẩm theo category (most frequent query)
    @Index(name = "idx_product_category_active", columnList = "category_id, is_active"),
    // ✅ Important: Trang chủ featured products với sắp xếp theo thời gian
    @Index(name = "idx_product_featured_active_created", columnList = "is_active, is_featured, created_at DESC"),
    // ✅ Essential: Sắp xếp theo rating cao -> thấp (đánh giá cao nhất)
    @Index(name = "idx_product_rating_reviews_desc", columnList = "is_active, average_rating DESC, review_count DESC"),
    // ✅ Useful: Sản phẩm của supplier (admin + user)
    @Index(name = "idx_product_supplier_active", columnList = "supplier_id, is_active"),
    // ✅ NEW: Join với ProductVariant optimization
    @Index(name = "idx_product_supplier_category", columnList = "supplier_id, category_id, is_active"),
    // ✅ NEW: Sản phẩm mới nhất (latest products) - sắp xếp DESC
    @Index(name = "idx_product_active_created_desc", columnList = "is_active, created_at DESC"),
    // ✅ NEW: Sản phẩm bán chạy (best sellers) - filter trước, sort sau
    @Index(name = "idx_product_active_sell_created", columnList = "is_active, sell_number DESC, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;
    
    @Column(name = "main_image_url", length = 255)
    private String mainImageUrl; // Cache ảnh chính để truy xuất nhanh, tự động sync từ productImages
    
    @Column(name = "sell_number")
    @Builder.Default
    private Long sellNumber = 0L; // Số lượt bán
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProductVariant> productVariants;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProductImage> productImages;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductReview> productReviews;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PromotionProduct> promotionProducts;
    
    /**
     * Lấy ảnh chính - ưu tiên từ cached field mainImageUrl
     * Fallback sang productImages nếu cache null
     */
    public String getMainImageUrlResolved() {
        // Ưu tiên dùng cached value
        if (mainImageUrl != null && !mainImageUrl.isEmpty()) {
            return mainImageUrl;
        }
        
        // Fallback: tính từ productImages
        if (productImages == null || productImages.isEmpty()) {
            return null;
        }
        
        return productImages.stream()
                .filter(ProductImage::getIsMain)
                .findFirst()
                .map(ProductImage::getMediaUrl)
                .orElseGet(() -> 
                    productImages.stream()
                            .min((img1, img2) -> img1.getDisplayOrder().compareTo(img2.getDisplayOrder()))
                            .map(ProductImage::getMediaUrl)
                            .orElse(null)
                );
    }
    
    /**
     * Sync mainImageUrl từ productImages (gọi sau khi thêm/sửa/xóa images)
     */
    public void syncMainImageUrl() {
        this.mainImageUrl = getMainImageUrlResolved();
    }
}
package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants", indexes = {
    // ✅ Critical: Lấy variants theo product (product detail page)
    @Index(name = "idx_variant_product_active", columnList = "product_id, is_active"),
    // ✅ Important: Default variant lookup (quick load)
    @Index(name = "idx_variant_product_default", columnList = "product_id, is_default"),
    // ✅ Useful: Stock management
    @Index(name = "idx_variant_stock", columnList = "stock_quantity"),
    // ✅ NEW: Join optimization for OrderDetail
    @Index(name = "idx_variant_product_stock_active", columnList = "product_id, stock_quantity, is_active"),
    // ✅ NEW: Quick lookup by ID & product
    @Index(name = "idx_variant_product_id", columnList = "product_id, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "sku", length = 100)  // ⚠️ Removed unique=true to allow duplicate SKUs
    private String sku;
    
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage; // % giảm giá (0-100)
    
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
    
    // Relationships
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;
    
    /**
     * Lấy tên đầy đủ của variant (Product name + Variant name)
     * Dùng cho OrderDetail và hiển thị
     */
    public String getFullName() {
        if (product == null) {
            return name;
        }
        return product.getName() + " - " + name;
    }
    
    /**
     * Lấy ảnh chính từ Product (cached field, rất nhanh)
     */
    public String getMainImageUrl() {
        if (product == null) {
            return null;
        }
        // Dùng cached field từ Product
        return product.getMainImageUrl();
    }
    
    /**
     * Lấy tất cả images của product (tất cả variants dùng chung)
     */
    public List<ProductImage> getImages() {
        if (product == null) {
            return new ArrayList<>();
        }
        return product.getProductImages() != null ? product.getProductImages() : new ArrayList<>();
    }
    
    /**
     * Tính giá sau khi giảm (final price)
     */
    public BigDecimal getDiscountedPrice() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return price;
        }
        
        // Tính % giảm: price * (100 - discountPercentage) / 100
        BigDecimal discountAmount = price.multiply(discountPercentage).divide(new BigDecimal("100"));
        return price.subtract(discountAmount);
    }
    
    /**
     * Tính số tiền được giảm
     */
    public BigDecimal getDiscountAmount() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        return price.multiply(discountPercentage).divide(new BigDecimal("100"));
    }
    
    /**
     * Kiểm tra có giảm giá không
     */
    public boolean hasDiscount() {
        return discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0;
    }
}
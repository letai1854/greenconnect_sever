package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
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
@Table(name = "order_details", indexes = {
    // ✅ Critical: Lấy chi tiết đơn hàng (order tracking)
    @Index(name = "idx_order_detail_order", columnList = "order_id"),
    // ✅ Important: Thống kê bán hàng theo variant
    @Index(name = "idx_order_detail_variant", columnList = "product_variant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant variant;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "product_name", length = 255)
    private String productName;
    
    @Column(name = "product_image_url", length = 255)
    private String productImageUrl;
    
    @Column(name = "unit", length = 50)
    private String unit;
    
    @Column(name = "original_price_per_unit", precision = 15, scale = 2)
    private BigDecimal originalPricePerUnit;
    
    @Column(name = "selling_price_per_unit", precision = 15, scale = 2)
    private BigDecimal sellingPricePerUnit;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    // Relationships
    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductReview> productReviews;
}
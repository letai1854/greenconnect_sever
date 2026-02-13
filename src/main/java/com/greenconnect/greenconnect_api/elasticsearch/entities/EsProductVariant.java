package com.greenconnect.greenconnect_api.elasticsearch.entities;

import java.math.BigDecimal;

// // === ELASTICSEARCH ANNOTATIONS (ĐÃ COMMENT OUT) ===
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EsProductVariant - DTO (trước đây là Elasticsearch nested object)
 * 
 * ✅ GIỮ NGUYÊN STRUCTURE ĐỂ BACKWARD COMPATIBILITY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
public class EsProductVariant {
    
    private String id;
    
    private String name;
    
    private String sku;
    
    private BigDecimal price;
    
    private BigDecimal discountPercentage;
    
    private Integer stockQuantity;
    
    private String unit;
    
    private Boolean isActive;
    
    private Boolean isDefault;
    
    // ===== COMPUTED FIELDS =====
    private BigDecimal discountedPrice;
    
    private BigDecimal discountAmount;
    
    /**
     * Helper method tính discounted price
     */
    public void calculateDiscountedPrice() {
        if (price == null) {
            this.discountedPrice = BigDecimal.ZERO;
            this.discountAmount = BigDecimal.ZERO;
            return;
        }
        
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            this.discountedPrice = price;
            this.discountAmount = BigDecimal.ZERO;
            return;
        }
        
        // Tính discount amount = price * discountPercentage / 100
        BigDecimal discount = price.multiply(discountPercentage).divide(new BigDecimal("100"));
        this.discountAmount = discount;
        this.discountedPrice = price.subtract(discount);
    }
}
package com.greenconnect.greenconnect_api.elasticsearch.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
// // === ELASTICSEARCH ANNOTATIONS (ĐÃ COMMENT OUT - dùng MySQL LIKE thay thế) ===
// import org.springframework.data.elasticsearch.annotations.DateFormat;
// import org.springframework.data.elasticsearch.annotations.Document;
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;
// import org.springframework.data.elasticsearch.annotations.InnerField;
// import org.springframework.data.elasticsearch.annotations.MultiField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EsProduct - DTO dùng cho search response (trước đây là Elasticsearch entity)
 * 
 * ✅ GIỮ NGUYÊN STRUCTURE ĐỂ BACKWARD COMPATIBILITY
 * ✅ KHÔNG CÒN DÙNG ELASTICSEARCH, CHỈ LÀ DTO THUẦN
 */
// @Document(indexName = "esproduct2")  // ĐÃ COMMENT OUT - không dùng Elasticsearch nữa
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class EsProduct {

    @Id
    private String id; // UUID as String
    
    // ===== BASIC PRODUCT INFO =====
private String name;
    private String description;
    
    private String slug;
    
    private Boolean isActive;
    
    private Boolean isFeatured;
    
    private BigDecimal averageRating;
    
    private Integer reviewCount;
    
    private Long sellNumber;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // ===== CATEGORY INFO =====
    private String categoryId;
    
    private String categoryName;
    
    private String categoryImageUrl;
    
    private Integer categoryDisplayOrder;
    
    private Boolean categoryIsActive;
    
    // ===== SUPPLIER INFO =====
    private String supplierId;
    
    private String supplierName;
    
    private String supplierEmail;
    
    private String supplierPhone;
    
    private String supplierLogoUrl;
    
    private Boolean supplierIsActive;
    
    // ===== VARIANTS INFO =====
    private List<EsProductVariant> variants;
    
    // ===== IMAGES INFO =====
    private List<EsProductImage> images;
    
    // ===== COMPUTED FIELDS =====
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private BigDecimal minDiscountedPrice;
    
    private BigDecimal maxDiscountedPrice;
    
    private BigDecimal defaultPrice;
    
    private BigDecimal defaultDiscountedPrice;
    
    private Integer totalStock;
    
    private Boolean inStock;
    
    private String mainImageUrl;
    
    // ===== SEARCH OPTIMIZATION =====
    private String searchText;
    
    /**
     * Helper method để build searchText
     */
    public void buildSearchText() {
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name).append(" ");
        if (description != null) sb.append(description).append(" ");
        if (categoryName != null) sb.append(categoryName).append(" ");
        if (supplierName != null) sb.append(supplierName);
        this.searchText = sb.toString().trim();
    }
}
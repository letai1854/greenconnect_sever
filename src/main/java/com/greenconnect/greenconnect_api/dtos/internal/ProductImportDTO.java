package com.greenconnect.greenconnect_api.dtos.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nội bộ để gom nhóm dữ liệu sản phẩm từ Excel
 * (Một sản phẩm có thể có nhiều variants từ nhiều dòng Excel)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImportDTO {
    
    // ===== THÔNG TIN CHUNG (chỉ lấy từ dòng đầu tiên) =====
    
    /**
     * Mã nhóm sản phẩm (để gom các dòng variant lại)
     */
    private String refId;
    
    /**
     * Tên sản phẩm
     */
    private String name;
    
    /**
     * Tên danh mục (từ Excel - cần convert sang UUID)
     */
    private String categoryName;
    
    /**
     * UUID danh mục (sau khi lookup DB)
     */
    private UUID categoryId;
    
    /**
     * Tên nhà cung cấp (từ Excel - cần convert sang UUID)
     */
    private String supplierName;
    
    /**
     * UUID nhà cung cấp (sau khi lookup DB)
     */
    private UUID supplierId;
    
    /**
     * Mô tả sản phẩm
     */
    private String description;
    
    /**
     * Slug (URL-friendly name)
     */
    private String slug;
    
    /**
     * Có hiển thị không
     */
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * Có nổi bật không
     */
    @Builder.Default
    private Boolean isFeatured = false;
    
    /**
     * Link ảnh chính của product
     */
    private String mainImageUrl;
    
    /**
     * Danh sách link ảnh phụ (cách nhau bởi dấu phẩy)
     */
    private String additionalImagesUrls;
    
    // ===== DANH SÁCH VARIANTS (gom từ nhiều dòng Excel) =====
    
    /**
     * Danh sách variants của sản phẩm
     */
    @Builder.Default
    private List<VariantImportDTO> variants = new ArrayList<>();
    
    /**
     * Danh sách lỗi validation (nếu có)
     */
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    
    /**
     * DTO cho một variant trong import
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantImportDTO {
        
        /**
         * Số dòng trong Excel (để báo lỗi)
         */
        private Integer rowNumber;
        
        /**
         * Tên variant (VD: Size M, Màu Đỏ)
         */
        private String name;
        
        /**
         * SKU (mã kho)
         */
        private String sku;
        
        /**
         * Giá gốc
         */
        private BigDecimal price;
        
        /**
         * Phần trăm giảm giá (0-100)
         */
        @Builder.Default
        private BigDecimal discountPercentage = BigDecimal.ZERO;
        
        /**
         * Số lượng tồn kho
         */
        private Integer stockQuantity;
        
        /**
         * Đơn vị tính (Cái, Hộp, Kg...)
         */
        private String unit;
        
        /**
         * Là variant mặc định không
         */
        @Builder.Default
        private Boolean isDefault = false;
    }
}

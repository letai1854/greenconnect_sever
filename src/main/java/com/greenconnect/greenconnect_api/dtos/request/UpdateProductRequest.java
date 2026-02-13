package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRequest {
    
    // Basic product info
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;
    
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;
    
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    private String slug; // Optional, sẽ auto-generate từ name nếu null
    
    private Boolean isActive;
    
    private Boolean isFeatured;
    
    // Category và Supplier changes
    private UUID categoryId;
    
    private UUID supplierId;
    
    // ⭐ Product images (dùng chung cho tất cả variants)
    @Size(max = 255, message = "URL ảnh chính không được vượt quá 255 ký tự")
    private String mainImageUrl;
    
    @Valid
    private List<UpdateProductImageRequest> images;
    
    // Danh sách variants cần update/add/remove
    @Valid
    private List<UpdateProductVariantRequest> variants;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProductVariantRequest {
        
        private UUID id; // null = tạo mới, có id = update existing
        
        @Size(max = 255, message = "Tên variant không được vượt quá 255 ký tự")
        private String name;
        
        @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
        private String sku; // Optional, sẽ auto-generate nếu null
        
        @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
        private BigDecimal price;
        
        @DecimalMin(value = "0.0", message = "Phần trăm giảm giá không được âm")
        @DecimalMax(value = "100.0", message = "Phần trăm giảm giá không được vượt quá 100%")
        private BigDecimal discountPercentage; // % giảm giá (0-100)
        
        @Min(value = 0, message = "Số lượng tồn kho không được âm")
        private Integer stockQuantity;
        
        @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
        private String unit;
        
        // ⚠️ mainImageUrl và images đã chuyển lên Product level
        // Variants không còn images riêng nữa
        
        private Boolean isActive;
        
        private Boolean isDefault;
        
        // Flag để xóa variant (nếu true thì sẽ xóa variant này)
        @Builder.Default
        private Boolean toDelete = false;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProductImageRequest {
        
        private UUID id; // null = tạo mới, có id = update existing
        
        @Size(max = 10, message = "Media type không hợp lệ")
        private String mediaType; // IMAGE hoặc VIDEO
        
        @Size(max = 255, message = "URL media không được vượt quá 255 ký tự")
        private String mediaUrl;
        
        @Min(value = 0, message = "Thứ tự hiển thị không được âm")
        private Integer displayOrder;
        
        private Boolean isMain; // Đánh dấu ảnh chính (optional)
        
        // Flag để xóa image (nếu true thì sẽ xóa image này)
        @Builder.Default
        private Boolean toDelete = false;
    }
}
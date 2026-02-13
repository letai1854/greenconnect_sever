package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class CreateProductRequest {
    
    @NotNull(message = "Category ID không được để trống")
    private UUID categoryId;
    
    @NotNull(message = "Supplier ID không được để trống")
    private UUID supplierId;
    
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;
    
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;
    
    @Size(max = 255, message = "Slug không được vượt quá 255 ký tự")
    private String slug; // Optional, sẽ auto-generate từ name nếu null
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isFeatured = false;
    
    // ⭐ THÊM MỚI: Main image URL cho Product (cached field)
    @Size(max = 255, message = "URL ảnh chính không được vượt quá 255 ký tự")
    private String mainImageUrl;
    
    // ⭐ THÊM MỚI: Danh sách images cho Product (dùng chung cho tất cả variants)
    @Valid
    private List<CreateProductImageRequest> images;
    
    // Danh sách variants của sản phẩm
    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 variant")
    @Valid
    private List<CreateProductVariantRequest> variants;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateProductVariantRequest {
        
        @NotBlank(message = "Tên variant không được để trống")
        @Size(max = 255, message = "Tên variant không được vượt quá 255 ký tự")
        private String name;
        
        @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
        private String sku; // Optional, sẽ auto-generate nếu null
        
        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
        private BigDecimal price;
        
        @DecimalMin(value = "0.0", message = "Phần trăm giảm giá không được âm")
        @DecimalMax(value = "100.0", message = "Phần trăm giảm giá không được vượt quá 100%")
        private BigDecimal discountPercentage; // % giảm giá (0-100)
        
        @NotNull(message = "Số lượng tồn kho không được để trống")
        @Min(value = 0, message = "Số lượng tồn kho không được âm")
        private Integer stockQuantity;
        
        @NotBlank(message = "Đơn vị không được để trống")
        @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
        private String unit;
        
        @Builder.Default
        private Boolean isActive = true;
        
        @Builder.Default
        private Boolean isDefault = false;
        
        // ⭐ ĐÃ XÓA: mainImageUrl và images (giờ variants dùng chung của Product)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateProductImageRequest {
        
        @NotBlank(message = "Media type không được để trống")
        private String mediaType; // IMAGE hoặc VIDEO
        
        @NotBlank(message = "URL media không được để trống")
        @Size(max = 255, message = "URL media không được vượt quá 255 ký tự")
        private String mediaUrl;
        
        @Min(value = 0, message = "Thứ tự hiển thị không được âm")
        @Builder.Default
        private Integer displayOrder = 0;
    }
}
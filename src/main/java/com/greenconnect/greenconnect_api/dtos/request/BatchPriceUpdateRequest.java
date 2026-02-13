package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho cập nhật giá và % giảm giá hàng loạt
 * Endpoint: POST /products/updateflash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchPriceUpdateRequest {
    
    /**
     * Danh sách các biến thể cần cập nhật giá/giảm giá
     */
    @NotEmpty(message = "Danh sách cập nhật không được rỗng")
    @Valid
    private List<VariantPriceUpdate> updates;
    
    /**
     * Chi tiết cập nhật cho từng biến thể sản phẩm
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantPriceUpdate {
        
        /**
         * ID của sản phẩm chứa biến thể
         */
        @NotNull(message = "productId không được null")
        private UUID productId;
        
        /**
         * ID của biến thể cần cập nhật
         */
        @NotNull(message = "variantId không được null")
        private UUID variantId;
        
        /**
         * Giá mới của biến thể (VND)
         * Nếu null, giữ nguyên giá cũ
         */
        @Positive(message = "Giá phải lớn hơn 0")
        private BigDecimal price;
        
        /**
         * % giảm giá mới (0-100)
         * Đặt 0 để xóa giảm giá
         * Nếu null, giữ nguyên
         */
        @DecimalMin(value = "0", message = "% giảm giá phải >= 0")
        @DecimalMax(value = "100", message = "% giảm giá phải <= 100")
        private BigDecimal discountPercentage;
    }
}

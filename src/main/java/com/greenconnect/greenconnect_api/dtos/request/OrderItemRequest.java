package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    
    @NotNull(message = "ID variant sản phẩm không được để trống")
    private UUID productVariantId;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 999, message = "Số lượng không được vượt quá 999")
    private Integer quantity;
    
    // ========== PRODUCT SNAPSHOT (Optional - Server can auto-fill) ==========
    private String productName;
    
    private String productImageUrl;
    
    private String unit;
    
    private BigDecimal originalPricePerUnit;
    
    private BigDecimal sellingPricePerUnit;
    
    @DecimalMin(value = "0.0", message = "Phần trăm giảm giá không được âm")
    @DecimalMax(value = "100.0", message = "Phần trăm giảm giá không được vượt quá 100%")
    @Digits(integer = 3, fraction = 2, message = "Phần trăm giảm giá không hợp lệ")
    private BigDecimal discountPercentage;
}
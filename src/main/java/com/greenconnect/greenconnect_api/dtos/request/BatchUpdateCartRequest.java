package com.greenconnect.greenconnect_api.dtos.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để cập nhật nhiều sản phẩm trong giỏ hàng cùng lúc (Batch Update)
 * Dùng cho Optimistic UI + Debounce pattern khi người dùng sửa nhiều sản phẩm rồi thoát màn hình
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateCartRequest {
    
    @NotEmpty(message = "Danh sách cập nhật không được rỗng")
    @Valid
    private List<CartItemUpdate> items;
    
    /**
     * Thông tin cập nhật cho từng cart item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemUpdate {
        
        @NotNull(message = "Cart item ID không được để trống")
        private UUID cartItemId;
        
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        private Integer quantity;
    }
}

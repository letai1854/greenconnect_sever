package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho toàn bộ giỏ hàng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    
    private List<CartItemResponse> items;
    private Integer totalItems;           // Tổng số item trong cart
    private Integer totalQuantity;        // Tổng số lượng sản phẩm
    private BigDecimal totalAmount;       // Tổng tiền (sử dụng discountPrice nếu có)
    private BigDecimal originalAmount;    // Tổng tiền gốc (không tính discount)
    private BigDecimal totalDiscount;     // Tổng số tiền được giảm
}
package com.greenconnect.greenconnect_api.dtos.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho batch update giỏ hàng
 * Trả về kết quả chi tiết cho từng item: thành công, lỗi, hoặc đã xóa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateCartResponse {
    
    private List<ItemUpdateResult> results;
    private int successCount;       // Số item cập nhật thành công
    private int failedCount;        // Số item cập nhật thất bại
    private int deletedCount;       // Số item đã xóa (quantity = 0)
    
    /**
     * Kết quả cập nhật cho từng cart item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemUpdateResult {
        private UUID cartItemId;
        private boolean success;
        private String status;          // "UPDATED", "DELETED", "FAILED"
        private String message;         // Chi tiết lỗi nếu có
        private Integer requestedQuantity;  // Số lượng yêu cầu
        private Integer actualQuantity;     // Số lượng thực tế (sau khi điều chỉnh theo stock)
        private Integer stockQuantity;      // Số lượng tồn kho hiện tại
        private CartItemResponse updatedItem; // Item đã cập nhật (null nếu xóa hoặc lỗi)
    }
}

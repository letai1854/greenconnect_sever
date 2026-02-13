package com.greenconnect.greenconnect_api.dtos.response;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả cập nhật giá hàng loạt
 * Endpoint: POST /products/updateflash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchPriceUpdateResponse {
    
    /**
     * Tổng số biến thể được yêu cầu cập nhật
     */
    private Integer totalUpdated;
    
    /**
     * Số biến thể cập nhật thành công
     */
    private Integer successCount;
    
    /**
     * Số biến thể cập nhật thất bại
     */
    private Integer failedCount;
    
    /**
     * Chi tiết các biến thể thất bại (nếu có)
     */
    private List<FailureDetail> failures;
    
    /**
     * Chi tiết lỗi cho từng biến thể thất bại
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FailureDetail {
        
        /**
         * ID của biến thể thất bại
         */
        private UUID variantId;
        
        /**
         * ID của sản phẩm chứa biến thể
         */
        private UUID productId;
        
        /**
         * Lý do thất bại
         */
        private String reason;
    }
}

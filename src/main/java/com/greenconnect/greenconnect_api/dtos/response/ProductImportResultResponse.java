package com.greenconnect.greenconnect_api.dtos.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả import sản phẩm từ Excel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImportResultResponse {
    
    /**
     * Tổng số sản phẩm được xử lý (đọc từ Excel)
     */
    private Integer totalProcessed;
    
    /**
     * Số sản phẩm tạo thành công
     */
    private Integer successCount;
    
    /**
     * Số sản phẩm thất bại
     */
    private Integer failedCount;
    
    /**
     * Chi tiết từng sản phẩm (thành công hoặc lỗi)
     */
    private List<ProductImportDetail> details;
    
    /**
     * Chi tiết kết quả import của từng sản phẩm
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductImportDetail {
        
        /**
         * Mã nhóm sản phẩm (Ref ID) từ Excel
         */
        private String refId;
        
        /**
         * Tên sản phẩm
         */
        private String productName;
        
        /**
         * Trạng thái: SUCCESS hoặc FAILED
         */
        private String status;
        
        /**
         * Thông báo (nếu thành công)
         */
        private String message;
        
        /**
         * Danh sách lỗi (nếu thất bại)
         */
        private List<String> errors;
    }
}

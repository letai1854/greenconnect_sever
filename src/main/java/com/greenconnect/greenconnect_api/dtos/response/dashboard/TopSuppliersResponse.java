package com.greenconnect.greenconnect_api.dtos.response.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho Top 5 nhà cung cấp có doanh số cao nhất
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopSuppliersResponse {
    
    private List<TopSupplier> suppliers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSupplier {
        private UUID supplierId;
        private String supplierName;
        private String supplierLogo;
        private Long productCount;          // Số sản phẩm
        private Long quantitySold;          // Tổng số lượng bán
        private BigDecimal revenue;         // Tổng doanh thu
    }
}

package com.greenconnect.greenconnect_api.dto.reports.analytical;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Tab Customer - Phân tích Khách hàng
 * ✅ Chuyển sang Double để tương thích tốt hơn với Frontend Charts
 * Đồng bộ với frontend: CustomerTab
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    
    // ==================== Phân bổ Khách hàng theo Khu vực ====================
    private Integer totalCustomers;                        // Tổng số khách hàng
    private List<CustomerByRegionData> customersByRegion;  // Phân bổ theo tỉnh
    
    // ==================== Tỷ lệ Khách hàng Mới ====================
    private Integer newCustomerCount;                      // Số khách hàng mới
    private Double newCustomerRate;                        // Tỷ lệ khách hàng mới (%)
    
    // ==================== Top Khách hàng VIP ====================
    private List<VipCustomerData> vipCustomers;            // Top 4 khách hàng VIP
    
    /**
     * Dữ liệu khách hàng theo khu vực (tỉnh)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerByRegionData {
        private String province;           // Tên tỉnh/thành phố
        private Integer count;             // Số lượng khách hàng
        private Double percentage;         // Tỷ lệ phần trăm
    }
    
    /**
     * Dữ liệu khách hàng VIP (chi tiêu cao)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VipCustomerData {
        private UUID customerId;
        private String name;               // Tên khách hàng
        private String email;              // Email
        private Double totalSpent;         // Tổng chi tiêu
        private Integer orderCount;        // Số đơn hàng
        private Double averageRating;      // Đánh giá trung bình (từ reviews)
    }
}

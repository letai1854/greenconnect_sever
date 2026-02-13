package com.greenconnect.greenconnect_api.dto.reports.analytical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response wrapper cho Analytical Report API
 * Bao gồm thông tin cache và rate limiting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticalReportResponse {
    
    private Boolean success;
    private String message;
    private TabType tab;                       // Tab đang được trả về
    private LocalDateTime timestamp;           // Thời điểm tạo response
    
    // ==================== Data cho từng tab ====================
    private OverviewResponse overview;
    private ProductResponse product;
    private CustomerResponse customer;
    
    // ==================== Cache & Rate Limiting Info ====================
    private CacheInfo cacheInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheInfo {
        private Boolean fromCache;              // Dữ liệu từ cache hay mới load
        private LocalDateTime cachedAt;         // Thời điểm cache
        private LocalDateTime expiresAt;        // Thời điểm hết hạn cache
        private Long remainingSeconds;          // Số giây còn lại đến khi có thể tải lại
        private String rateLimitMessage;        // Thông báo về rate limit
    }
}

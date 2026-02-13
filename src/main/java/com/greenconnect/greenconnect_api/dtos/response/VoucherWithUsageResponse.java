package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.greenconnect.greenconnect_api.enums.VoucherType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho voucher kèm trạng thái sử dụng của user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherWithUsageResponse {
    
    private UUID id;
    
    @JsonProperty("voucher_code")
    private String voucherCode;
    
    private String description;
    
    @JsonProperty("voucher_type")
    private VoucherType voucherType;
    
    @JsonProperty("discount_value")
    private BigDecimal discountValue;
    
    @JsonProperty("max_discount_amount")
    private BigDecimal maxDiscountAmount;
    
    @JsonProperty("min_order_value")
    private BigDecimal minOrderValue;
    
    @JsonProperty("max_usage_count")
    private Integer maxUsageCount;
    
    @JsonProperty("used_count")
    private Integer usedCount;
    
    @JsonProperty("start_date")
    private LocalDateTime startDate;
    
    @JsonProperty("end_date")
    private LocalDateTime endDate;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("usage_limit_per_user")
    private Integer usageLimitPerUser;
    
    // ========== USER-SPECIFIC FIELDS ==========
    
    /**
     * Số lần user này đã sử dụng voucher
     */
    @JsonProperty("user_used_count")
    private Integer userUsedCount;
    
    /**
     * User đã sử dụng hết lượt voucher này chưa
     * true = đã hết lượt, không thể dùng nữa
     * false = còn lượt, có thể dùng
     */
    @JsonProperty("is_fully_used_by_user")
    private Boolean isFullyUsedByUser;
    
    /**
     * Voucher có còn khả dụng không (tổng hợp tất cả điều kiện)
     * - is_active = true
     * - chưa hết hạn (start_date <= now <= end_date)
     * - còn lượt sử dụng chung (used_count < max_usage_count)
     * - user chưa dùng hết lượt cá nhân
     */
    @JsonProperty("is_available")
    private Boolean isAvailable;
    
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
}

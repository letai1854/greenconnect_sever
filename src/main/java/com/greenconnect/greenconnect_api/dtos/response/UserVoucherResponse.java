package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.VoucherStatus;
import com.greenconnect.greenconnect_api.enums.VoucherType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucherResponse {
    
    private UUID id;
    private String voucherCode;
    private String description;
    private VoucherType voucherType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Integer maxUsageCount;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimitPerUser;
    private LocalDateTime createdDate;
    
    // User-specific fields
    private VoucherStatus status;
    private Boolean canUse;
    private String statusMessage;
    private Integer userUsedCount;
    private LocalDateTime lastUsedTime;
    private Integer remainingUsage;
    
    // Calculated fields
    private Long daysUntilExpiry;
    private Long daysUntilStart;
}
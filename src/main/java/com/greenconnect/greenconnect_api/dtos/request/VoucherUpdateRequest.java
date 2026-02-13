package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.greenconnect.greenconnect_api.enums.VoucherType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateRequest {
    
    @Size(min = 3, max = 50, message = "Mã voucher phải từ 3-50 ký tự")
    private String voucherCode;
    
    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;
    
    private VoucherType voucherType;
    
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.01", message = "Số tiền giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;
    
    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;
    
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải lớn hơn 0")
    private Integer maxUsageCount;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    @Min(value = 1, message = "Giới hạn sử dụng trên người dùng phải lớn hơn 0")
    private Integer usageLimitPerUser;
    
    private Boolean isActive;
}
package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.greenconnect.greenconnect_api.enums.VoucherType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCreateRequest {
    
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 50, message = "Mã voucher phải từ 3-50 ký tự")
    private String voucherCode;
    
    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;
    
    @NotNull(message = "Loại voucher không được để trống")
    private VoucherType voucherType;
    
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.01", message = "Số tiền giảm tối đa phải lớn hơn 0")
    private BigDecimal maxDiscountAmount;
    
    @NotNull(message = "Giá trị đơn hàng tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue = BigDecimal.ZERO;
    
    @NotNull(message = "Số lượt sử dụng tối đa không được để trống")
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải lớn hơn 0")
    private Integer maxUsageCount;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    @Future(message = "Ngày kết thúc phải là thời điểm trong tương lai")
    private LocalDateTime endDate;
    
    @NotNull(message = "Giới hạn sử dụng trên người dùng không được để trống")
    @Min(value = 1, message = "Giới hạn sử dụng trên người dùng phải lớn hơn 0")
    private Integer usageLimitPerUser = 1;
    
    private Boolean isActive = true;
}
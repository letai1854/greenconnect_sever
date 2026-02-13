package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusRequest {
    
    @NotBlank(message = "Trạng thái thanh toán không được để trống")
    @Pattern(
        regexp = "CHUA_THANH_TOAN|DA_THANH_TOAN|THAT_BAI",
        message = "Trạng thái thanh toán không hợp lệ. Chỉ chấp nhận: CHUA_THANH_TOAN, DA_THANH_TOAN, THAT_BAI"
    )
    private String newPaymentStatus;
    
    private String note;
}

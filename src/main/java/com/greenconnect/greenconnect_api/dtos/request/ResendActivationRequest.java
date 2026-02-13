package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu gửi lại email kích hoạt
 * Dùng khi user chưa kích hoạt hoặc token đã hết hạn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendActivationRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}

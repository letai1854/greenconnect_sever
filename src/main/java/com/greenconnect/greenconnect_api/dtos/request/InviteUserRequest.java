package com.greenconnect.greenconnect_api.dtos.request;

import java.util.Set;

import com.greenconnect.greenconnect_api.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho yêu cầu mời người dùng mới
 * Dùng bởi Admin để tạo tài khoản và gửi email kích hoạt với một hoặc nhiều roles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteUserRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotEmpty(message = "Vai trò không được để trống")
    private Set<Role> roles; // Cho phép nhiều roles: CUSTOMER_SUPPORT, ORDER_MANAGER, SHIPPER, etc.
    
    // Optional fields
    private String fullName; // Nếu không có, sẽ lấy phần trước @ của email
}

package com.greenconnect.greenconnect_api.dtos.request.Suppliers;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {
    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(min = 3, max = 255, message = "Tên nhà cung cấp phải có độ dài từ 3 đến 255 ký tự")
    private String name;

    private String description; // Mô tả không bắt buộc

    @URL(message = "URL logo không hợp lệ")
    private String logoUrl; // URL logo không bắt buộc, nhưng nếu có phải đúng định dạng URL

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 20, message = "Số điện thoại phải có độ dài từ 10 đến 20 ký tự")
    private String phoneNumber;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive = true; // Mặc định là true khi tạo mới
}

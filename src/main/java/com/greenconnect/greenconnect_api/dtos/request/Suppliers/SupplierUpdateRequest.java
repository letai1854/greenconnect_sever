package com.greenconnect.greenconnect_api.dtos.request.Suppliers;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierUpdateRequest {

    @Size(min = 3, max = 255, message = "Tên nhà cung cấp phải có độ dài từ 3 đến 255 ký tự")
    private String name;

    private String description;

    @URL(message = "URL logo không hợp lệ")
    private String logoUrl;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Email(message = "Email không đúng định dạng")
    private String email;

    @Size(min = 10, max = 20, message = "Số điện thoại phải có độ dài từ 10 đến 20 ký tự")
    private String phoneNumber;

    private Boolean isActive;
}
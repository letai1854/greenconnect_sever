package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordResponse {
    private String message;
    private boolean success;
    
    public static ResetPasswordResponse success() {
        return new ResetPasswordResponse(
            "Mật khẩu đã được thay đổi thành công!",
            true
        );
    }
    
    public static ResetPasswordResponse error(String message) {
        return new ResetPasswordResponse(message, false);
    }
}
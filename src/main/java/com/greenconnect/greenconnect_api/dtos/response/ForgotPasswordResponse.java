package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponse {
    private String message;
    private String email;
    private boolean success;
    
    public static ForgotPasswordResponse success(String email) {
        return new ForgotPasswordResponse(
            "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư trong vòng 3 phút.",
            email,
            true
        );
    }
    
    public static ForgotPasswordResponse error(String message) {
        return new ForgotPasswordResponse(message, null, false);
    }
}
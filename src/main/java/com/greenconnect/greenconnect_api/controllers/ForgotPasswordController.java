package com.greenconnect.greenconnect_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.ForgotPasswordRequest;
import com.greenconnect.greenconnect_api.dtos.request.ResetPasswordRequest;
import com.greenconnect.greenconnect_api.dtos.request.VerifyEmailOtpRequest;
import com.greenconnect.greenconnect_api.dtos.response.ForgotPasswordResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResetPasswordResponse;
import com.greenconnect.greenconnect_api.services.OtpService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/forgot-password")
public class ForgotPasswordController {
    
    @Autowired
    private OtpService otpService;
    
    /**
     * Gửi mã OTP đến email người dùng
     * <p>⚠️ Chỉ áp dụng cho user đăng ký bằng LOCAL (email + password)</p>
     * <p>❌ Từ chối nếu user đăng ký bằng GOOGLE (Firebase)</p>
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ForgotPasswordResponse> sendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            // Kiểm tra provider của user trước khi gửi OTP
            boolean isGoogleUser = otpService.isGoogleProvider(request.getEmail());
            
            if (isGoogleUser) {
                return ResponseEntity.badRequest()
                    .body(ForgotPasswordResponse.error(
                        "Tài khoản Google không cần đặt lại mật khẩu. Vui lòng đăng nhập bằng Google."
                    ));
            }
            
            // Gửi OTP cho user LOCAL
            otpService.generateAndSendOtp(request.getEmail());
            return ResponseEntity.ok(ForgotPasswordResponse.success(request.getEmail()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ForgotPasswordResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Xác thực OTP (chỉ kiểm tra, không reset password)
     * <p>Endpoint này dùng để kiểm tra OTP có hợp lệ không trước khi cho user nhập mật khẩu mới</p>
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ResetPasswordResponse> verifyOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        try {
            // Kiểm tra OTP có hợp lệ không (chưa hết hạn và khớp với OTP đã gửi)
            boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
            
            if (isValid) {
                return ResponseEntity.ok(
                    new ResetPasswordResponse("Mã OTP hợp lệ. Bạn có thể đặt lại mật khẩu.", true)
                );
            } else {
                return ResponseEntity.badRequest()
                    .body(ResetPasswordResponse.error("Mã OTP không chính xác hoặc đã hết hạn."));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ResetPasswordResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Xác thực OTP và đặt lại mật khẩu
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            // Xác thực OTP và đặt lại mật khẩu
            otpService.verifyOtpAndResetPassword(
                request.getEmail(), 
                request.getOtp(), 
                request.getNewPassword()
            );
            
            return ResponseEntity.ok(ResetPasswordResponse.success());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ResetPasswordResponse.error(e.getMessage()));
        }
    }
}
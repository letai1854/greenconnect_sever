package com.greenconnect.greenconnect_api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.ActivateAccountRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.InvitationResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.entities.InvitationToken;
import com.greenconnect.greenconnect_api.services.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller xử lý activation (kích hoạt tài khoản)
 * <p>Public endpoints - KHÔNG cần authentication</p>
 */
@Slf4j
@RestController
@RequestMapping("/public/activation")
@RequiredArgsConstructor
@Validated
public class ActivationController {
    
    private final InvitationService invitationService;
    
    /**
     * Verify activation token (kiểm tra token có hợp lệ không)
     * <p>Frontend gọi endpoint này khi user click vào link activation để check token trước khi hiện form password</p>
     * 
     * @param token Activation token từ URL parameter
     * @return ApiResponse chứa thông tin token và email
     */
    @GetMapping("/verify-token")
    public ResponseEntity<ApiResponse<InvitationResponse>> verifyToken(
            @RequestParam("token") String token) {
        
        log.info("Verify activation token: {}***", token.substring(0, Math.min(8, token.length())));
        
        InvitationToken invitationToken = invitationService.verifyActivationToken(token);
        
        InvitationResponse response = InvitationResponse.builder()
                .email(invitationToken.getEmail())
                .fullName(invitationToken.getFullName())
                .tokenSent(true)
                .expiryDate(invitationToken.getExpiryDate())
                .message("Token hợp lệ. Vui lòng tạo mật khẩu để kích hoạt tài khoản.")
                .build();
        
        return ResponseEntity.ok(
                ApiResponse.<InvitationResponse>builder()
                        .code(HttpStatus.OK.value())
                        .message("Token hợp lệ")
                        .data(response)
                        .build()
        );
    }
    
    /**
     * Activate account (kích hoạt tài khoản + auto login)
     * <p>User nhập password và submit form activation</p>
     * 
     * @param request DTO chứa token, password, confirmPassword
     * @return LoginResponse với access token và refresh token để auto-login
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<LoginResponse>> activateAccount(
            @Valid @RequestBody ActivateAccountRequest request) {
        
        log.info("Activate account with token: {}***", 
                request.getToken().substring(0, Math.min(8, request.getToken().length())));
        
        LoginResponse loginResponse = invitationService.activateAccount(request);
        
        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .code(HttpStatus.OK.value())
                        .message("Kích hoạt tài khoản thành công")
                        .data(loginResponse)
                        .build()
        );
    }
}

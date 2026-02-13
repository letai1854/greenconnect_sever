package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.ActivateAccountRequest;
import com.greenconnect.greenconnect_api.dtos.request.InviteUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.ResendActivationRequest;
import com.greenconnect.greenconnect_api.dtos.response.InvitationResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginInitialResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.entities.InvitationToken;

/**
 * Service quản lý lời mời và kích hoạt tài khoản
 */
public interface InvitationService {
    
    /**
     * Admin mời người dùng mới (tạo tài khoản + gửi email kích hoạt)
     * 
     * @param request DTO chứa email, fullName, role
     * @param adminId UUID của admin đang thực hiện mời
     * @return InvitationResponse chứa thông tin lời mời
     */
    InvitationResponse inviteUser(InviteUserRequest request, UUID adminId);
    
    /**
     * Gửi lại email kích hoạt (vô hiệu hóa token cũ, tạo token mới)
     * 
     * @param request DTO chứa email
     * @param adminId UUID của admin đang thực hiện resend
     * @return InvitationResponse chứa thông tin token mới
     */
    InvitationResponse resendActivationEmail(ResendActivationRequest request, UUID adminId);
    
    /**
     * User kích hoạt tài khoản (verify token + set password + auto login)
     * 
     * @param request DTO chứa token, password, confirmPassword
     * @return LoginResponse chứa access token và refresh token để auto-login
     */
    LoginResponse activateAccount(ActivateAccountRequest request);
    
    /**
     * User kích hoạt tài khoản với Google (Firebase)
     * Tương tự activateAccount() nhưng dùng cho provider GOOGLE
     * 
     * @param token Activation token từ email
     * @param fullName Tên user từ Google profile
     * @param photoURL Avatar URL từ Google
     * @param providerId Google User ID
     * @return LoginResponse chứa access token và refresh token để auto-login
     */
    LoginResponse activateAccountWithGoogle(String token, String fullName, String photoURL, String providerId);
    
    /**
     * Verify token kích hoạt (kiểm tra hợp lệ, chưa hết hạn, chưa sử dụng)
     * Dùng để validate token trước khi cho user nhập password
     * 
     * @param token String token từ URL
     * @return InvitationToken entity nếu hợp lệ
     */
    InvitationToken verifyActivationToken(String token);
    
    /**
     * Lấy danh sách user đang chờ kích hoạt (PENDING_ACTIVATION)
     * 
     * @param pageable Phân trang
     * @return Page chứa danh sách InvitationResponse
     */
    Page<InvitationResponse> getPendingActivations(Pageable pageable);
    
    /**
     * Cleanup job: Xóa các token đã hết hạn (chạy định kỳ)
     * 
     * @return Số lượng token đã xóa
     */
    int cleanupExpiredTokens();

    // ========== 2FA (TOTP) ACTIVATION FLOW ==========

    /**
     * User kích hoạt tài khoản với password - với luồng 2FA
     * <p>Tương tự activateAccount() nhưng trả về 2FA response</p>
     * <p>Không trả về tokens ngay - yêu cầu verify OTP trước</p>
     *
     * @param request DTO chứa token, password, confirmPassword
     * @return LoginInitialResponse với QR code hoặc yêu cầu OTP (KHÔNG có tokens)
     */
    LoginInitialResponse activateAccountWithTotp(ActivateAccountRequest request);

    /**
     * User kích hoạt tài khoản với Google - với luồng 2FA
     * <p>Tương tự activateAccountWithGoogle() nhưng trả về 2FA response</p>
     * <p>Không trả về tokens ngay - yêu cầu verify OTP trước</p>
     *
     * @param token Activation token từ email
     * @param fullName Tên user từ Google profile
     * @param photoURL Avatar URL từ Google
     * @param providerId Google User ID
     * @return LoginInitialResponse với QR code hoặc yêu cầu OTP (KHÔNG có tokens)
     */
    LoginInitialResponse activateAccountGoogleWithTotp(String token, String fullName, String photoURL, String providerId);
}

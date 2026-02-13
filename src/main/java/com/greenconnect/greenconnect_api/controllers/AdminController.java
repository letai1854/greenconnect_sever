package com.greenconnect.greenconnect_api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.ActivateAccountRequest;
import com.greenconnect.greenconnect_api.dtos.request.FilterCustomersRequest;
import com.greenconnect.greenconnect_api.dtos.request.FilterUsersRequest;
import com.greenconnect.greenconnect_api.dtos.request.GetCustomersRequest;
import com.greenconnect.greenconnect_api.dtos.request.GetNonCustomerUsersRequest;
import com.greenconnect.greenconnect_api.dtos.request.InviteUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.LoginFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.LoginRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterRequestGoogleManage;
import com.greenconnect.greenconnect_api.dtos.request.RegisterRequestManage;
import com.greenconnect.greenconnect_api.dtos.request.ResendActivationRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateUserStatusRequest;
import com.greenconnect.greenconnect_api.dtos.request.VerifyOtpRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.FilterCustomersResponse;
import com.greenconnect.greenconnect_api.dtos.response.FilterUsersResponse;
import com.greenconnect.greenconnect_api.dtos.response.GetCustomersResponse;
import com.greenconnect.greenconnect_api.dtos.response.GetNonCustomerUsersResponse;
import com.greenconnect.greenconnect_api.dtos.response.InvitationResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginInitialResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.UpdateUserStatusResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.enums.UserStatus;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.services.InvitationService;
import com.greenconnect.greenconnect_api.services.UserService;
import com.greenconnect.greenconnect_api.utils.JwtUtils;
import com.greenconnect.greenconnect_api.websocket.notification.RoleChangeNotificationService;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Controller - Đơn giản cho việc quản lý users.
 * <p>Chỉ admin mới có quyền sử dụng các endpoints này.</p>
 */
    @RestController
    @RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Getter
@Setter
public class AdminController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final RoleChangeNotificationService roleChangeNotificationService;
    private final InvitationService invitationService;

    /**
     * Admin update user theo UUID.
     * <p>Endpoint đơn giản nhất cho admin update roles và thông tin user khác.</p>
     */
    // @PutMapping("/users/{userId}")
    // public ApiResponse<UserResponse> updateUserById(@PathVariable UUID userId,
    //                                                @Valid @RequestBody UpdateUserRequest request,
    //                                                @RequestHeader("Authorization") String authHeader) {
    //     // Extract token và kiểm tra admin
    //     String token = authHeader.substring(7);
    //     Set<Role> userRoles = jwtUtils.getRolesFromToken(token);
        
    //     if (!userRoles.contains(Role.ADMIN)) {
    //         throw new BusinessException(ErrorCode.UNAUTHORIZED);
    //     }
        
    //     log.info("Admin updating user: {}", userId);
    //     UserResponse response = userService.updateUser(userId, request, userRoles);
    //     return ResponseUtil.success(response, "Admin cập nhật user thành công");
    // }

    /**
     * Admin update user theo Email.
     * <p>Dễ sử dụng hơn khi admin biết email của user.</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim) chứ không chỉ primary role.</p>
     */
    @PutMapping("/users/email/{email:.+}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<UserResponse> updateUserByEmail(@PathVariable String email,
                                                      @Valid @RequestBody UpdateUserRequest request
                                                    ) {
        log.info("📝 [ADMIN UPDATE USER] Admin đang cập nhật user: {}", email);
        
        // Cập nhật user trong database
        UserResponse response = userService.updateUserByEmail(email, request);
        
        // ⭐⭐ NẾU CÓ THAY ĐỔI ROLE → GỬI THÔNG BÁO WEBSOCKET
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            log.info("🔔 [ROLE CHANGED] Phát hiện thay đổi role cho user '{}', gửi thông báo real-time", email);
            
            roleChangeNotificationService.notifyRoleChange(
                    email,
                    response.getRoles(),
                    response.getPrimaryRole()
            );
        }
        
        // ⭐⭐ NẾU TÀI KHOẢN BỊ VÔ HIỆU HÓA → GỬI THÔNG BÁO FORCE LOGOUT
        if (request.getStatus() != null && request.getStatus() == UserStatus.INACTIVE) {
            log.info("🚨 [ACCOUNT DISABLED] User '{}' bị vô hiệu hóa, gửi thông báo force logout", email);
            roleChangeNotificationService.notifyAccountDisabled(email);
        }
        
        return ResponseUtil.success(response, "Admin cập nhật user thành công");
    }
    
    // ==================== INVITATION SYSTEM ENDPOINTS ====================
    
    /**
     * Admin mời user mới (tạo tài khoản + gửi email kích hoạt)
     * <p>Chỉ ADMIN được phép mời user với một hoặc nhiều roles</p>
     * <p>✅ Hỗ trợ nhiều roles: CUSTOMER_SUPPORT, ORDER_MANAGER, SHIPPER, etc.</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim).</p>
     * 
     * @param request DTO chứa email, roles (Set), fullName (optional)
     * @param authentication Spring Security authentication object để lấy admin ID
     * @return InvitationResponse chứa thông tin lời mời
     */
    @PostMapping("/users/invite")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<InvitationResponse> inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            Authentication authentication) {
        
        log.info("📧 [ADMIN INVITE] Admin mời user: {}", request.getEmail());
        
        try {
            // Lấy admin ID từ authentication principal (JWT subject = email)
            String adminEmail = authentication.getName();
            UserResponse adminUser = userService.getUserByEmail(adminEmail);
            
            log.info("📧 [ADMIN INVITE] Admin ID: {}, Admin Email: {}", adminUser.getId(), adminEmail);
            
            InvitationResponse response = invitationService.inviteUser(request, adminUser.getId());
            
            log.info("✅ [ADMIN INVITE] THÀNH CÔNG - Email đã gửi tới: {}", request.getEmail());
            log.info("✅ [ADMIN INVITE] Response: token_sent={}, expiry_date={}", response.isTokenSent(), response.getExpiryDate());
            
            return ResponseUtil.success(response, "Đã gửi lời mời kích hoạt tới " + request.getEmail());
            
        } catch (Exception e) {
            log.error("❌ [ADMIN INVITE] LỖI - Không gửi được email: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/google-login")
    public ApiResponse<LoginInitialResponse> firebaseLogin(@Valid @RequestBody LoginFirebaseRequest request) {
        log.info("🔐 [GOOGLE LOGIN] User đang đăng nhập bằng Google");
        
        LoginInitialResponse response = userService.loginFirebaseUserWithTotp(request);
        
        if (response.getIs2FAActivated()) {
            return ResponseUtil.success(response, "Vui lòng nhập mã OTP từ Google Authenticator");
        } else {
            return ResponseUtil.success(response, "Vui lòng quét QR code và nhập mã OTP");
        }
    }
    
    @PostMapping("/login")
    public ApiResponse<LoginInitialResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔐 [LOGIN] User đang đăng nhập bằng email/password");
        
        LoginInitialResponse response = userService.loginUserWithTotp(request);
        
        if (response.getIs2FAActivated()) {
            return ResponseUtil.success(response, "Vui lòng nhập mã OTP từ Google Authenticator");
        } else {
            return ResponseUtil.success(response, "Vui lòng quét QR code và nhập mã OTP");
        }
    }

    /**
     * Xác thực OTP và hoàn tất đăng nhập
     * <p>Endpoint này nhận mã OTP 6 số từ Google Authenticator</p>
     * <p>Nếu OTP đúng → trả về access token + refresh token</p>
     * <p>Nếu OTP sai → trả về error code 4890</p>
     */
    @PostMapping("/verify-otp")
    public ApiResponse<LoginResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("🔐 [VERIFY OTP] Đang xác thực OTP cho user: {}", request.getUserId());
        
        LoginResponse response = userService.verifyOtpAndLogin(request);
        
        return ResponseUtil.success(response, "Đăng nhập thành công");
    }

    /**
     * Reset Google Authenticator (2FA) cho user khi mất điện thoại.
     * <p>⚠️ CHỈ ADMIN MỚI CÓ QUYỀN reset 2FA cho user</p>
     * <p>🔄 Reset totp_secret về NULL và is_2fa_activated về FALSE</p>
     * <p>🚨 User sẽ phải thiết lập lại 2FA ở lần đăng nhập tiếp theo</p>
     * <p>🔔 Gửi thông báo force logout qua WebSocket cho user (security measure)</p>
     * 
     * @param email Email của user cần reset 2FA
     * @param authentication Spring Security authentication object để lấy admin ID
     * @return UserResponse chứa thông tin user sau khi reset
     */
    @DeleteMapping("/users/{email:.+}/reset-2fa")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> reset2FA(
            @PathVariable String email,
            Authentication authentication) {
        
        log.info("🔄 [RESET 2FA] Admin đang reset Google Authenticator cho user: {}", email);
        
        try {
            // Lấy thông tin admin đang thực hiện
            String adminEmail = authentication.getName();
            UserResponse adminUser = userService.getUserByEmail(adminEmail);
            
            log.info("🔄 [RESET 2FA] Admin thực hiện: {} (ID: {})", adminEmail, adminUser.getId());
            
            // Reset 2FA cho user
            UserResponse response = userService.reset2FA(email);
            
            log.info("✅ [RESET 2FA] THÀNH CÔNG - User '{}' đã được reset 2FA", email);
            
            // 🔔 GỬI THÔNG BÁO FORCE LOGOUT (security measure)
            // User phải đăng nhập lại và thiết lập 2FA mới
            log.info("🚨 [RESET 2FA] Gửi thông báo force logout cho user '{}'", email);
            roleChangeNotificationService.notifyAccountDisabled(email);
            
            return ResponseUtil.success(response, 
                    "Đã reset Google Authenticator thành công. User phải đăng nhập lại và thiết lập 2FA mới.");
            
        } catch (Exception e) {
            log.error("❌ [RESET 2FA] LỖI - Không thể reset 2FA: {}", e.getMessage(), e);
            throw e;
        }
    }

    


    /**
     * Admin gửi lại email kích hoạt cho user chưa kích hoạt
     * <p>Vô hiệu hóa token cũ, tạo token mới, gửi email mới</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim).</p>
     * 
     * @param request DTO chứa email
     * @param authentication Spring Security authentication object
     * @return InvitationResponse chứa thông tin token mới
     */
    @PostMapping("/users/resend-activation")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<InvitationResponse> resendActivationEmail(
            @Valid @RequestBody ResendActivationRequest request,
            Authentication authentication) {
        
        log.info("🔄 [ADMIN RESEND] Admin gửi lại activation email cho: {}", request.getEmail());
        
        try {
            String adminEmail = authentication.getName();
            UserResponse adminUser = userService.getUserByEmail(adminEmail);
            
            log.info("🔄 [ADMIN RESEND] Admin ID: {}, Admin Email: {}", adminUser.getId(), adminEmail);
            
            InvitationResponse response = invitationService.resendActivationEmail(request, adminUser.getId());
            
            log.info("✅ [ADMIN RESEND] THÀNH CÔNG - Email đã gửi lại tới: {}", request.getEmail());
            log.info("✅ [ADMIN RESEND] Response: token_sent={}, expiry_date={}", response.isTokenSent(), response.getExpiryDate());
            
            return ResponseUtil.success(response, "Đã gửi lại email kích hoạt tới " + request.getEmail());
            
        } catch (Exception e) {
            log.error("❌ [ADMIN RESEND] LỖI - Không gửi được email: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Lấy danh sách users đang chờ kích hoạt (status = PENDING_ACTIVATION)
     * <p>Hỗ trợ phân trang và sắp xếp</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim).</p>
     * 
     * @param pageable Thông tin phân trang (page, size, sort)
     * @return Page chứa danh sách users pending activation
     */
    @GetMapping("/users/pending-activation")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<InvitationResponse>> getPendingActivations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        log.info("📋 [ADMIN LIST] Admin xem danh sách pending activation users");
        
        Page<InvitationResponse> response = invitationService.getPendingActivations(pageable);
        
        return ResponseUtil.success(response, 
                "Có " + response.getTotalElements() + " user đang chờ kích hoạt");
    }




        
    /**
     * ========== ACTIVATION ENDPOINTS (Admin Invite Flow) ==========
     * User nhận lời mời từ admin, click link activation, nhập password để kích hoạt tài khoản
     */
    
    /**
     * Kích hoạt tài khoản với email + password (từ link email invitation)
     * <p>Frontend redirect từ: http://localhost:3000/manage/activate?token=UUID</p>
     * <p>User nhập password → POST endpoint này → trả về QR code hoặc yêu cầu OTP</p>
     * <p>⚠️ KHÔNG trả về tokens - tuân theo luồng 2FA</p>
     * 
     * @param request chứa token (từ email), password, confirmPassword
     * @return LoginInitialResponse với QR code hoặc yêu cầu OTP (KHÔNG có tokens)
     */
    @PostMapping("/manage/activate-account")
    public ApiResponse<LoginInitialResponse> activateAccountWithPassword(
            @Valid @RequestBody RegisterRequestManage request) {
        log.info("🔐 [ACTIVATE ACCOUNT] User kích hoạt tài khoản bằng password với token: {}***", 
                request.getToken().substring(0, Math.min(8, request.getToken().length())));
        
        try {
            // Kiểm tra password khớp nhau
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                log.warn("❌ [ACTIVATE] Mật khẩu không khớp");
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Mật khẩu không khớp nhau");
            }
            
            // Chuyển RegisterRequestManage sang ActivateAccountRequest
            ActivateAccountRequest activateRequest = new ActivateAccountRequest(
                    request.getToken(),
                    request.getPassword(),
                    request.getConfirmPassword()
            );
            
            // Kích hoạt tài khoản và trả về response với 2FA status
            LoginInitialResponse response = invitationService.activateAccountWithTotp(activateRequest);
            log.info("✅ [ACTIVATE] Kích hoạt tài khoản thành công");
            
            if (response.getIs2FAActivated()) {
                return ResponseUtil.success(response, "Vui lòng nhập mã OTP từ Google Authenticator");
            } else {
                return ResponseUtil.success(response, "Vui lòng quét QR code và nhập mã OTP");
            }
            
        } catch (AppException e) {
            log.error("❌ [ACTIVATE] AppException: {}", e.getMessage());
            throw e;  // Throw lại để GlobalExceptionHandler xử lý
        } catch (Exception e) {
            log.error("❌ [ACTIVATE] Exception: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lỗi kích hoạt tài khoản: " + e.getMessage());
        }
    }
    
    /**
     * Kích hoạt tài khoản với Google (token từ Firebase)
     * <p>Tương tự như /manage/activate-account nhưng dùng Google auth</p>
     * <p>⚠️ KHÔNG trả về tokens - tuân theo luồng 2FA</p>
     * 
     * @param request chứa token, providerId (Google UID), fullName, photoURL
     * @return LoginInitialResponse với QR code hoặc yêu cầu OTP (KHÔNG có tokens)
     */
    @PostMapping("/manage/activate-google")
    public ApiResponse<LoginInitialResponse> activateAccountWithGoogle(
            @Valid @RequestBody RegisterRequestGoogleManage request) {
        log.info("🔐 [ACTIVATE GOOGLE] User kích hoạt tài khoản bằng Google với token: {}***", 
                request.getToken().substring(0, Math.min(8, request.getToken().length())));
        
        try {
            // Kích hoạt tài khoản Google và trả về response với 2FA status
            LoginInitialResponse response = invitationService.activateAccountGoogleWithTotp(
                    request.getToken(),
                    request.getFullName(),
                    request.getPhotoURL(),
                    request.getProviderId()
            );
            log.info("✅ [ACTIVATE GOOGLE] Kích hoạt tài khoản Google thành công");
            
            if (response.getIs2FAActivated()) {
                return ResponseUtil.success(response, "Vui lòng nhập mã OTP từ Google Authenticator");
            } else {
                return ResponseUtil.success(response, "Vui lòng quét QR code và nhập mã OTP");
            }
            
        } catch (AppException e) {
            log.error("❌ [ACTIVATE GOOGLE] AppException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ [ACTIVATE GOOGLE] Exception: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INVALID_REQUEST, "Lỗi kích hoạt tài khoản Google: " + e.getMessage());
        }
    }
    
    /**
     * Lấy danh sách users không phải CUSTOMER role.
     * <p>Admin xem danh sách tất cả staff: ORDER_MANAGER, PRODUCT_MANAGER, MARKETING_MANAGER, etc.</p>
     * <p>✅ Hỗ trợ phân trang, sắp xếp theo createdAt giảm dần mặc định</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim)</p>
     * 
     * @param request GetNonCustomerUsersRequest chứa pageNumber, pageSize, sort, sortDirection
     * @return GetNonCustomerUsersResponse chứa danh sách users không phải CUSTOMER
     */
    @GetMapping("/users/non-customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GetNonCustomerUsersResponse> getNonCustomerUsers(
            @Valid GetNonCustomerUsersRequest request,
            Authentication authentication) {
        
        log.info("📋 [ADMIN GET NON-CUSTOMERS] Lấy danh sách users không phải CUSTOMER role");
        
        // Lấy userId của admin đang thực hiện truy vấn
        String adminEmail = authentication.getName();
        UserResponse adminUser = userService.getUserByEmail(adminEmail);
        
        GetNonCustomerUsersResponse response = 
                userService.getNonCustomerUsers(request, adminUser.getId());
        
        log.info("✅ [ADMIN GET NON-CUSTOMERS] Tìm thấy {} users (đã loại bỏ admin {})", 
                response.getTotalElements(), adminEmail);
        
        return ResponseUtil.success(response, 
                "Lấy danh sách " + response.getUsers().size() + " users không phải CUSTOMER thành công");
    }
    
    /**
     * Lọc danh sách users theo nhiều tiêu chí nâng cao.
     * <p>Admin sử dụng endpoint này để lọc users theo: trạng thái, vai trò, ngày tạo</p>
     * <p>✅ Hỗ trợ phân trang, sắp xếp với mặc định là createdAt giảm dần (DESC)</p>
     * <p>✅ Kiểm tra tất cả roles (scope claim)</p>
     * <p>✅ Tự động loại bỏ admin đang thực hiện truy vấn khỏi kết quả</p>
     * 
     * @param request FilterUsersRequest chứa tiêu chí lọc
     * @param authentication Spring Security authentication object
     * @return FilterUsersResponse chứa danh sách users lọc
     */
    @GetMapping("/users/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FilterUsersResponse> filterUsers(
            @Valid FilterUsersRequest request,
            Authentication authentication) {
        
        log.info("🔍 [ADMIN FILTER USERS] Lọc danh sách users với tiêu chí: status={}, roles={}, startDate={}, endDate={}", 
                request.getStatus(), request.getRoles(), request.getStartDate(), request.getEndDate());
        
        // Lấy userId của admin đang thực hiện truy vấn
        String adminEmail = authentication.getName();
        UserResponse adminUser = userService.getUserByEmail(adminEmail);
        
        FilterUsersResponse response = 
                userService.filterUsers(request, adminUser.getId());
        
        log.info("✅ [ADMIN FILTER USERS] Tìm thấy {} users (đã loại bỏ admin {})", 
                response.getTotalElements(), adminEmail);
        
        return ResponseUtil.success(response, 
                "Lọc danh sách " + response.getUsers().size() + " users thành công");
    }

    // ==================== CUSTOMER USERS MANAGEMENT ENDPOINTS ====================
    
    /**
     * Lấy danh sách khách hàng (CUSTOMER role).
     * <p>ADMIN và CUSTOMER_SUPPORT sử dụng endpoint này để xem danh sách tất cả khách hàng</p>
     * <p>✅ Hỗ trợ phân trang, sắp xếp theo createdAt giảm dần mặc định</p>
     * <p>✅ Kiểm tra quyền: ADMIN hoặc CUSTOMER_SUPPORT</p>
     * 
     * @param request GetCustomersRequest chứa pageNumber, pageSize, sort, sortDirection
     * @return GetCustomersResponse chứa danh sách customers
     */
    @GetMapping("/users/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT')")
    public ApiResponse<GetCustomersResponse> getCustomers(
            @Valid GetCustomersRequest request) {
        
        log.info("📋 [GET CUSTOMERS] Lấy danh sách khách hàng (CUSTOMER role)");
        
        GetCustomersResponse response = userService.getCustomers(request);
        
        log.info("✅ [GET CUSTOMERS] Tìm thấy {} customers", response.getTotalElements());
        
        return ResponseUtil.success(response, 
                "Lấy danh sách " + response.getCustomers().size() + " khách hàng thành công");
    }
    
    /**
     * Lọc danh sách khách hàng (CUSTOMER role) theo nhiều tiêu chí nâng cao.
     * <p>ADMIN và CUSTOMER_SUPPORT sử dụng endpoint này để lọc customers theo: tên/email/điện thoại, trạng thái, ngày tạo</p>
     * <p>✅ Hỗ trợ phân trang, sắp xếp với mặc định là createdAt giảm dần (DESC)</p>
     * <p>✅ Kiểm tra quyền: ADMIN hoặc CUSTOMER_SUPPORT</p>
     * 
     * @param request FilterCustomersRequest chứa tiêu chí lọc
     * @return FilterCustomersResponse chứa danh sách customers lọc
     */
    @GetMapping("/users/customers/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT')")
    public ApiResponse<FilterCustomersResponse> filterCustomers(
            @Valid FilterCustomersRequest request) {
        
        log.info("🔍 [FILTER CUSTOMERS] Lọc danh sách khách hàng với tiêu chí: status={}, startDate={}, endDate={}", 
                request.getStatus(), request.getStartDate(), request.getEndDate());
        
        FilterCustomersResponse response = userService.filterCustomers(request);
        
        log.info("✅ [FILTER CUSTOMERS] Tìm thấy {} customers", response.getTotalElements());
        
        return ResponseUtil.success(response, 
                "Lọc danh sách " + response.getCustomers().size() + " khách hàng thành công");
    }
    
    /**
     * Cập nhật trạng thái tài khoản user (ACTIVE/INACTIVE).
     * <p>ADMIN và CUSTOMER_SUPPORT sử dụng endpoint này để kích hoạt hoặc vô hiệu hóa tài khoản</p>
     * <p>✅ Chỉ cho phép cập nhật: ACTIVE ↔ INACTIVE</p>
     * <p>❌ Không cho phép cập nhật sang: PENDING_ACTIVATION hoặc BANNED</p>
     * <p>⚠️ Khi set INACTIVE → User sẽ bị force logout qua WebSocket</p>
     * 
     * @param email Email của user cần cập nhật trạng thái
     * @param request UpdateUserStatusRequest chứa status mới (ACTIVE/INACTIVE) và lý do
     * @param authentication Spring Security authentication object
     * @return UpdateUserStatusResponse chứa thông tin user và trạng thái cũ/mới
     */
    @PutMapping("/users/{email:.+}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT')")
    public ApiResponse<UpdateUserStatusResponse> updateUserStatus(
            @PathVariable String email,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        
        log.info("🔄 [UPDATE USER STATUS] Cập nhật trạng thái tài khoản: {}", email);
        log.info("🔄 [UPDATE USER STATUS] Trạng thái mới: {}, Lý do: {}", request.getStatus(), request.getReason());
        
        // Lấy email của admin/support đang thực hiện
        String updatedByEmail = authentication.getName();
        
        UpdateUserStatusResponse response = userService.updateUserStatus(email, request, updatedByEmail);
        
        String message = String.format(
                "Đã cập nhật trạng thái tài khoản từ %s sang %s",
                response.getPreviousStatus(),
                response.getNewStatus()
        );
        
        log.info("✅ [UPDATE USER STATUS] {} cho user: {}", message, email);
        
        return ResponseUtil.success(response, message);
    }
}

    
    // ==================== END - CUSTOMER USERS MANAGEMENT ENDPOINTS ====================
    

package com.greenconnect.greenconnect_api.services.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.ActivateAccountRequest;
import com.greenconnect.greenconnect_api.dtos.request.InviteUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.ResendActivationRequest;
import com.greenconnect.greenconnect_api.dtos.response.InvitationResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginInitialResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.entities.InvitationToken;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.UserRole;
import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.mappers.UserMapper;
import com.greenconnect.greenconnect_api.repositories.InvitationTokenRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.repositories.UserRoleRepository;
import com.greenconnect.greenconnect_api.services.EmailService;
import com.greenconnect.greenconnect_api.services.InvitationService;
import com.greenconnect.greenconnect_api.services.RefreshTokenService;
import com.greenconnect.greenconnect_api.utils.JwtUtils;
import com.greenconnect.greenconnect_api.utils.PasswordUtils;
import com.greenconnect.greenconnect_api.utils.TotpUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý lời mời và kích hoạt tài khoản
 * Tách biệt hoàn toàn với logic OtpCode hiện có
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {
    
    private final InvitationTokenRepository invitationTokenRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;  // ⭐ THÊM EmailService
    
    @Value("${app.invitation.token-expiry-hours:24}")
    private int tokenExpiryHours;
    
    @Value("${app.invitation.base-url:http://localhost:3000}")
    private String baseUrl;
    
    /**
     * Admin mời người dùng mới
     */
    @Override
    @Transactional
    public InvitationResponse inviteUser(InviteUserRequest request, UUID adminId) {
        log.info("Admin {} mời user mới: {} với roles: {}", adminId, request.getEmail(), request.getRoles());
        
        String email = request.getEmail().trim().toLowerCase();
        
        // 1. Kiểm tra email đã tồn tại trong hệ thống chưa
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Email đã được sử dụng: {}", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 2. Kiểm tra có token đang active không (email đã được mời nhưng chưa kích hoạt)
        boolean hasActiveToken = invitationTokenRepository.existsActiveTokenByEmail(email, LocalDateTime.now());
        if (hasActiveToken) {
            log.warn("Email {} đã có lời mời chưa kích hoạt", email);
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        // 3. Validate roles hợp lệ (không cho phép tạo ADMIN, CUSTOMER)
        Set<Role> validatedRoles = validateAndParseRoles(request.getRoles());
        
        // 4. Tạo fullName từ request hoặc random (Manager + số)
        String fullName = request.getFullName() != null && !request.getFullName().trim().isEmpty()
                ? request.getFullName().trim()
                : "Manager" + (new java.util.Random().nextInt(90000) + 10000);
        
        // 5. Tạo User với status PENDING_ACTIVATION (chưa có password)
        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .status(UserStatus.PENDING_ACTIVATION)
                .provider(Provider.LOCAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Đã tạo user với ID {} và status PENDING_ACTIVATION", savedUser.getId());
        
        // 6. Assign roles cho user
        validatedRoles.forEach(role -> {
            UserRole userRole = UserRole.builder()
                    .user(savedUser)
                    .role(role)
                    .active(true)
                    .assignedAt(LocalDateTime.now())
                    .build();
            
            userRoleRepository.save(userRole);
            log.info("Đã assign role {} cho user {}", role, savedUser.getId());
        });
        
        // 7. Tạo activation token
        String token = generateActivationToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
        
        InvitationToken invitationToken = InvitationToken.builder()
                .email(email)
                .fullName(fullName)
                .tokenHash(tokenHash)
                .expiryDate(expiryDate)
                .isUsed(false)
                .invitedByAdminId(adminId)
                .createdAt(LocalDateTime.now())
                .build();
        
        invitationTokenRepository.save(invitationToken);
        log.info("Đã tạo activation token cho email: {}, hết hạn lúc: {}", email, expiryDate);
        
        // 8. Gửi email kích hoạt (TODO: integrate email service)
        String activationUrl = buildActivationUrl(token);
        log.info("📧 [INVITATION EMAIL] Chuẩn bị gửi email kích hoạt...");
        log.info("📧 [INVITATION EMAIL] Email: {}, URL: {}", email, activationUrl);
        try {
            sendActivationEmail(email, fullName, activationUrl);
            log.info("✅ [INVITATION EMAIL] Email kích hoạt đã gửi thành công!");
        } catch (Exception e) {
            log.error("❌ [INVITATION EMAIL] LỖI GỬI EMAIL: {}", e.getMessage(), e);
            throw e;
        }
        
        // 9. Trả về response
        String rolesStr = validatedRoles.stream()
                .map(Role::name)
                .collect(Collectors.joining(", "));
        
        return InvitationResponse.builder()
                .email(email)
                .fullName(fullName)
                .role(rolesStr)
                .tokenSent(true)
                .expiryDate(expiryDate)
                .invitedAt(LocalDateTime.now())
                .invitedByAdmin("Admin ID: " + adminId)
                .message("Đã gửi email kích hoạt tới " + email)
                .build();
    }
    
    /**
     * Gửi lại email kích hoạt
     */
    @Override
    @Transactional
    public InvitationResponse resendActivationEmail(ResendActivationRequest request, UUID adminId) {
        log.info("Admin {} resend activation email cho: {}", adminId, request.getEmail());
        
        String email = request.getEmail().trim().toLowerCase();
        
        // 1. Kiểm tra user tồn tại và có status PENDING_ACTIVATION
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
            log.warn("User {} không ở trạng thái PENDING_ACTIVATION", email);
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        
        // 2. Vô hiệu hóa tất cả token cũ của email
        int invalidatedCount = invitationTokenRepository.invalidateAllByEmail(email);
        log.info("Đã vô hiệu hóa {} token cũ của email {}", invalidatedCount, email);
        
        // 3. Tạo token mới
        String token = generateActivationToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(tokenExpiryHours);
        
        InvitationToken invitationToken = InvitationToken.builder()
                .email(email)
                .fullName(user.getFullName())
                .tokenHash(tokenHash)
                .expiryDate(expiryDate)
                .isUsed(false)
                .invitedByAdminId(adminId)
                .createdAt(LocalDateTime.now())
                .build();
        
        invitationTokenRepository.save(invitationToken);
        log.info("Đã tạo token mới cho email: {}, hết hạn lúc: {}", email, expiryDate);
        
        // 4. Gửi email kích hoạt
        String activationUrl = buildActivationUrl(token);
        log.info("📧 [RESEND EMAIL] Chuẩn bị gửi email kích hoạt...");
        log.info("📧 [RESEND EMAIL] Email: {}, URL: {}", email, activationUrl);
        try {
            sendActivationEmail(email, user.getFullName(), activationUrl);
            log.info("✅ [RESEND EMAIL] Email kích hoạt đã gửi thành công!");
        } catch (Exception e) {
            log.error("❌ [RESEND EMAIL] LỖI GỬI EMAIL: {}", e.getMessage(), e);
            throw e;
        }
        
        // 5. Lấy role của user
        Set<Role> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        
        String roleStr = roles.stream()
                .map(Role::name)
                .collect(Collectors.joining(", "));
        
        return InvitationResponse.builder()
                .email(email)
                .fullName(user.getFullName())
                .role(roleStr)
                .tokenSent(true)
                .expiryDate(expiryDate)
                .invitedAt(LocalDateTime.now())
                .invitedByAdmin("Admin ID: " + adminId)
                .message("Đã gửi lại email kích hoạt tới " + email)
                .build();
    }
    
    /**
     * User kích hoạt tài khoản + auto login
     */
    @Override
    @Transactional
    public LoginResponse activateAccount(ActivateAccountRequest request) {
        log.info("Kích hoạt tài khoản với token: {}***", request.getToken().substring(0, 8));
        
        // 1. Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        
        // 2. Verify token
        InvitationToken invitationToken = verifyActivationToken(request.getToken());
        
        // 3. Lấy user theo email
        User user = userRepository.findByEmailIgnoreCase(invitationToken.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
            log.warn("User {} không ở trạng thái PENDING_ACTIVATION", user.getEmail());
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        
        // 4. Set password và active user
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("Đã kích hoạt tài khoản cho user: {}", user.getEmail());
        
        // 5. Đánh dấu token đã sử dụng
        invitationToken.setIsUsed(true);
        invitationToken.setActivatedAt(LocalDateTime.now());
        invitationToken.setActivatedUserId(user.getId());
        
        invitationTokenRepository.save(invitationToken);
        log.info("Đã đánh dấu token đã sử dụng");
        
        // 6. Auto login - tạo JWT tokens
        Set<Role> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        
        Role primaryRole = roles.stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS));
        
        // Tạo access token với user info
        String accessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles,
                primaryRole,
                user.getFullName()
        );
        
        // Tạo refresh token entity trong database
        LocalDateTime refreshTokenExpiry = LocalDateTime.now().plusDays(30);
        String refreshTokenStr = jwtUtils.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                primaryRole.name(),
                user.getFullName()
        );
        
        refreshTokenService.createRefreshToken(
                user,
                refreshTokenStr,
                "activation-auto-login",
                refreshTokenExpiry
        );
        
        log.info("User {} đã kích hoạt và tự động đăng nhập", user.getEmail());
        
        // 7. Build response với user info
        long accessTokenExpiresIn = 900L; // 15 minutes in seconds
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60; // 30 days in seconds
        
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(roles) // Set<Role>
                .build();
        
        return LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenStr)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .user(userResponse)
                .build();
    }
    
    /**
     * Verify token kích hoạt
     */
    @Override
    @Transactional(readOnly = true)
    public InvitationToken verifyActivationToken(String token) {
        log.info("Verify activation token");
        
        String tokenHash = hashToken(token);
        LocalDateTime now = LocalDateTime.now();
        
        InvitationToken invitationToken = invitationTokenRepository
                .findByTokenHash(tokenHash, now)
                .orElseThrow(() -> {
                    log.warn("Token không hợp lệ hoặc đã hết hạn");
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });
        
        if (invitationToken.getIsUsed()) {
            log.warn("Token đã được sử dụng");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        if (invitationToken.getExpiryDate().isBefore(now)) {
            log.warn("Token đã hết hạn");
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }
        
        log.info("Token hợp lệ cho email: {}", invitationToken.getEmail());
        return invitationToken;
    }
    
    /**
     * Lấy danh sách pending activation users
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InvitationResponse> getPendingActivations(Pageable pageable) {
        log.info("Lấy danh sách user chờ kích hoạt");
        
        Page<User> users = userRepository.findByStatus(UserStatus.PENDING_ACTIVATION, pageable);
        
        return users.map(user -> {
            // Lấy token active mới nhất (nếu có)
            LocalDateTime expiryDate = invitationTokenRepository
                    .findLatestActiveByEmail(user.getEmail(), LocalDateTime.now())
                    .map(InvitationToken::getExpiryDate)
                    .orElse(null);
            
            Set<String> roles = user.getUserRoles().stream()
                    .map(ur -> ur.getRole().name())
                    .collect(Collectors.toSet());
            
            return InvitationResponse.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(String.join(", ", roles))
                    .tokenSent(expiryDate != null)
                    .expiryDate(expiryDate)
                    .invitedAt(user.getCreatedAt())
                    .build();
        });
    }
    
    /**
     * User kích hoạt tài khoản với Google (Firebase)
     * Tương tự activateAccount() nhưng dùng Google/Firebase provider
     */
    @Override
    @Transactional
    public LoginResponse activateAccountWithGoogle(String token, String fullName, String photoURL, String providerId) {
        log.info("Kích hoạt tài khoản với Google, token: {}***", token.substring(0, 8));
        
        // 1. Verify token
        InvitationToken invitationToken = verifyActivationToken(token);
        
        // 2. Lấy user theo email
        User user = userRepository.findByEmailIgnoreCase(invitationToken.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
            log.warn("User {} không ở trạng thái PENDING_ACTIVATION", user.getEmail());
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        
        // 3. Update user info từ Google
        user.setFullName(fullName != null ? fullName : user.getFullName());
        user.setAvatarUrl(photoURL);
        user.setProvider(Provider.GOOGLE);
        user.setProviderId(providerId);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        // Không cần set password vì Google handle auth
        
        userRepository.save(user);
        log.info("Đã kích hoạt tài khoản Google cho user: {}", user.getEmail());
        
        // 4. Đánh dấu token đã sử dụng
        invitationToken.setIsUsed(true);
        invitationToken.setActivatedAt(LocalDateTime.now());
        invitationToken.setActivatedUserId(user.getId());
        invitationTokenRepository.save(invitationToken);
        
        // 5. Auto login - tạo JWT tokens
        Set<Role> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
        
        Role primaryRole = roles.stream().findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATUS));
        
        String accessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles,
                primaryRole,
                user.getFullName()
        );
        
        String refreshTokenStr = jwtUtils.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                primaryRole.name(),
                user.getFullName()
        );
        
        refreshTokenService.createRefreshToken(
                user,
                refreshTokenStr,
                "google-activation-auto-login",
                LocalDateTime.now().plusDays(30)
        );
        
        log.info("User {} đã kích hoạt với Google và tự động đăng nhập", user.getEmail());
        
        // 6. Build response
        long accessTokenExpiresIn = 900L;
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60;
        
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(roles)
                .build();
        
        return LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenStr)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .user(userResponse)
                .build();
    }
    
    /**
     * Cleanup expired tokens
     */
    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        log.info("Cleanup expired invitation tokens");
        int deleted = invitationTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Đã xóa {} token hết hạn", deleted);
        return deleted;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Validate và parse multiple roles từ Set<Role>
     */
    private Set<Role> validateAndParseRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Phải có ít nhất một role");
        }
        
        return roles.stream()
                .peek(role -> {
                    // Chỉ không cho phép tạo CUSTOMER qua invitation
                    // ADMIN được phép tạo bởi ADMIN khác
                    if (role == Role.CUSTOMER) {
                        throw new IllegalArgumentException("Không thể mời user với role CUSTOMER");
                    }
                })
                .collect(Collectors.toSet());
    }
    
    /**
     * Generate random activation token (UUID format)
     */
    private String generateActivationToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Hash token bằng SHA-256 để lưu vào database
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Không thể hash token", e);
            throw new RuntimeException("Lỗi hash token", e);
        }
    }
    
    /**
     * Build activation URL
     */
    private String buildActivationUrl(String token) {
        return baseUrl + "/manage/activate?token=" + token;
    }
    
    /**
     * Gửi email kích hoạt (sử dụng EmailService để gửi mail thực sự)
     */
    private void sendActivationEmail(String email, String fullName, String activationUrl) {
        log.info("📧 [SEND ACTIVATION EMAIL] Vào method sendActivationEmail");
        String subject = "🎉 Kích hoạt tài khoản GreenConnect";
        
        String emailContent = String.format(
            "Chào %s,\n\n" +
            "Bạn đã được mời tham gia GreenConnect. Vui lòng click vào link dưới để kích hoạt tài khoản của bạn:\n\n" +
            "%s\n\n" +
            "Link này có hiệu lực trong %d giờ.\n\n" +
            "Nếu bạn không yêu cầu tham gia GreenConnect, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ GreenConnect",
            fullName, activationUrl, tokenExpiryHours
        );
        
        log.info("📧 [SEND ACTIVATION EMAIL] Subject: {}", subject);
        log.info("📧 [SEND ACTIVATION EMAIL] To: {}", email);
        log.info("📧 [SEND ACTIVATION EMAIL] emailService is null? {}", emailService == null);
        
        try {
            log.info("📧 [SEND ACTIVATION EMAIL] Gọi emailService.sendPlainEmail()");
            if (emailService != null) {
                emailService.sendPlainEmail(email, subject, emailContent);
                log.info("✅ [EMAIL SENT] Email kích hoạt đã gửi thành công tới: {}", email);
            } else {
                log.error("❌ [EMAIL ERROR] emailService is NULL!");
                throw new RuntimeException("emailService is NULL");
            }
        } catch (Exception e) {
            log.error("❌ [EMAIL ERROR] Lỗi gửi email kích hoạt cho: {}, Error: {}", email, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email kích hoạt: " + e.getMessage());
        }
    }

    // ========== 2FA (TOTP) ACTIVATION IMPLEMENTATION ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginInitialResponse activateAccountWithTotp(ActivateAccountRequest request) {
        log.info("🔐 [ACTIVATE WITH TOTP] Bắt đầu kích hoạt tài khoản với password");
        
        // 1. Verify token
        InvitationToken invitationToken = verifyActivationToken(request.getToken());
        
        // 2. Lấy user từ token
        User user = userRepository.findByEmailIgnoreCase(invitationToken.getEmail())
                .orElseThrow(() -> {
                    log.error("❌ User không tìm thấy: {}", invitationToken.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 3. Kiểm tra trạng thái tài khoản - chỉ cho phép PENDING_ACTIVATION
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Không thể kích hoạt tài khoản bị vô hiệu hóa: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.ACTIVE) {
            log.warn("❌ Tài khoản đã được kích hoạt trước đó: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);
        }
        
        // 4. Hash password và lưu
        String hashedPassword = PasswordUtils.hashPassword(request.getPassword());
        user.setPasswordHash(hashedPassword);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("✅ Password đã được lưu cho user: {}", user.getEmail());
        
        // 5. Vô hiệu hóa token (không được sử dụng lại)
        invitationToken.setIsUsed(true);
        invitationTokenRepository.save(invitationToken);
        log.info("✅ Token kích hoạt đã được vô hiệu hóa");
        
        // 6. Trả về 2FA response (tương tự loginUserWithTotp flow)
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            // Chưa kích hoạt 2FA → tạo secret key và trả về QR code
            return generateQrCodeResponseForActivation(user);
        } else {
            // Đã kích hoạt 2FA → yêu cầu nhập OTP
            return requireOtpResponseForActivation(user);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginInitialResponse activateAccountGoogleWithTotp(String token, String fullName, String photoURL, String providerId) {
        log.info("🔐 [ACTIVATE GOOGLE WITH TOTP] Bắt đầu kích hoạt tài khoản Google");
        
        // 1. Verify token
        InvitationToken invitationToken = verifyActivationToken(token);
        
        // 2. Lấy user từ token
        User user = userRepository.findByEmailIgnoreCase(invitationToken.getEmail())
                .orElseThrow(() -> {
                    log.error("❌ User không tìm thấy: {}", invitationToken.getEmail());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 3. Kiểm tra trạng thái tài khoản - chỉ cho phép PENDING_ACTIVATION
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Không thể kích hoạt tài khoản bị vô hiệu hóa: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.ACTIVE) {
            log.warn("❌ Tài khoản đã được kích hoạt trước đó: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVATED);
        }
        
        // 4. Update user info từ Google
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (photoURL != null && !photoURL.trim().isEmpty()) {
            user.setAvatarUrl(photoURL);
        }
        
        // 5. Hash providerId và lưu
        String hashedProviderId = PasswordUtils.hashPassword(providerId);
        user.setProviderId(hashedProviderId);
        user.setProvider(Provider.GOOGLE);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("✅ Google profile đã được lưu cho user: {}", user.getEmail());
        
        // 6. Vô hiệu hóa token
        invitationToken.setIsUsed(true);
        invitationTokenRepository.save(invitationToken);
        log.info("✅ Token kích hoạt đã được vô hiệu hóa");
        
        // 7. Trả về 2FA response
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            // Chưa kích hoạt 2FA → tạo secret key và trả về QR code
            return generateQrCodeResponseForActivation(user);
        } else {
            // Đã kích hoạt 2FA → yêu cầu nhập OTP
            return requireOtpResponseForActivation(user);
        }
    }

    // ========== HELPER METHODS FOR TOTP ACTIVATION ==========

    /**
     * Tạo response với QR code cho activation flow
     */
    private LoginInitialResponse generateQrCodeResponseForActivation(User user) {
        log.info("📱 [GENERATE QR] Tạo QR code cho activation user: {}", user.getEmail());
        
        // 1. Tạo secret key nếu chưa có
        String secretKey = user.getSecretKey2FA();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = TotpUtils.generateSecretKey();
            user.setSecretKey2FA(secretKey);
            userRepository.save(user);
            log.info("✅ Đã tạo secret key mới cho user: {}", user.getEmail());
        }
        
        // 2. Tạo QR code URL
        String qrCodeUrl = TotpUtils.generateQrCodeUrl(user.getEmail(), secretKey);
        
        // 3. Trả về response
        return LoginInitialResponse.builder()
                .userId(user.getId())
                .is2FAActivated(false)
                .qrCodeUrl(qrCodeUrl)
                .secretKey(secretKey)
                .message("Vui lòng quét QR code bằng Google Authenticator và nhập mã OTP")
                .user(UserMapper.toUserResponse(user))
                .build();
    }

    /**
     * Tạo response yêu cầu nhập OTP cho activation flow
     */
    private LoginInitialResponse requireOtpResponseForActivation(User user) {
        log.info("🔒 [REQUIRE OTP] User đã kích hoạt 2FA: {}", user.getEmail());
        
        return LoginInitialResponse.builder()
                .userId(user.getId())
                .is2FAActivated(true)
                .qrCodeUrl(null)
                .secretKey(null)
                .message("Vui lòng nhập mã OTP từ Google Authenticator")
                .user(UserMapper.toUserResponse(user))
                .build();
    }
}


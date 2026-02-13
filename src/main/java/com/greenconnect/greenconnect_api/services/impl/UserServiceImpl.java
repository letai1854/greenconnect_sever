package com.greenconnect.greenconnect_api.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

import com.greenconnect.greenconnect_api.dtos.request.LoginRequest;
import com.greenconnect.greenconnect_api.dtos.request.LoginFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.RefreshTokenRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.VerifyOtpRequest;
import com.greenconnect.greenconnect_api.dtos.request.FilterUsersRequest;
import com.greenconnect.greenconnect_api.dtos.response.LoginInitialResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.dtos.response.FilterUsersResponse;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.UserDevice;
import com.greenconnect.greenconnect_api.entities.UserRole;
import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.mappers.UserMapper;
import com.greenconnect.greenconnect_api.repositories.UserDeviceRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.repositories.UserRoleRepository;
import com.greenconnect.greenconnect_api.repositories.FcmTokenRepository;
import com.greenconnect.greenconnect_api.services.UserService;
import com.greenconnect.greenconnect_api.services.RefreshTokenService;
import com.greenconnect.greenconnect_api.utils.JwtUtils;
import com.greenconnect.greenconnect_api.utils.PasswordUtils;
import com.greenconnect.greenconnect_api.utils.TotpUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của UserService interface.
 * <p>Xử lý các business logic liên quan đến User management.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final com.greenconnect.greenconnect_api.websocket.notification.RoleChangeNotificationService roleChangeNotificationService;
    private final com.greenconnect.greenconnect_api.repositories.AddressRepository addressRepository;
    private final com.greenconnect.greenconnect_api.repositories.FavoriteRepository favoriteRepository;
    private final FcmTokenRepository fcmTokenRepository;
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse registerUser(RegisterRequest request) {
        log.info("Bắt đầu đăng ký user với email: {}", request.getEmail());
        
        String email = request.getEmail().trim();
        
        // 1. Kiểm tra email đã tồn tại chưa
        validateEmailNotExists(email);
        
        // 2. Convert RegisterRequest to User entity (tự động set provider=LOCAL)
        User user = UserMapper.fromRegisterRequest(request);
        
        // 3. Ensure provider is LOCAL
        user.setProvider(Provider.LOCAL);
        
        // 4. Save user và assign role CUSTOMER
        User savedUser = saveUserWithDefaultRole(user);
        
        log.info("Đã đăng ký user thành công với ID: {} và provider: LOCAL", savedUser.getId());
        
        // 5. Tạo JWT tokens sau khi save
        return createLoginResponse(savedUser);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse registerFirebaseUser(RegisterFirebaseRequest request) {
        log.info("Bắt đầu đăng ký Firebase user với email: {}", request.getEmail());
        
        String email = request.getEmail().toLowerCase().trim();
        
        // 1. Kiểm tra email đã tồn tại chưa
        validateEmailNotExists(email);
        
        // 2. Convert RegisterFirebaseRequest to User entity (tự động set provider từ request)
        User user = UserMapper.fromFirebaseRequest(request);
        
        // 3. Ensure provider is GOOGLE (or set from request provider field if exist)
        user.setProvider(Provider.GOOGLE);
        
        // 4. Save user và assign role CUSTOMER
        User savedUser = saveUserWithDefaultRole(user);
        
        log.info("Đã đăng ký Firebase user thành công với ID: {} và provider: GOOGLE", savedUser.getId());
        
        // 5. Tạo JWT tokens sau khi save
        return createLoginResponse(savedUser);
    }
    
    // ==================== HELPER METHODS FOR REGISTRATION ====================
    
    /**
     * Validate email chưa được sử dụng.
     */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Email đã tồn tại: {}", email);
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }
    
    /**
     * Save user và assign default CUSTOMER role.
     */
    private User saveUserWithDefaultRole(User user) {
        // 0. Generate userCode (sequential number starting from 100000)
        user.setUserCode(generateUserCode());
        
        // 1. Save user to database
        User savedUser = userRepository.save(user);
        log.info("Đã tạo user với ID: {} và userCode: {}", savedUser.getId(), savedUser.getUserCode());
        
        // 2. Assign default CUSTOMER role
        UserRole customerRole = UserRole.builder()
                .user(savedUser)
                .role(Role.CUSTOMER)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(customerRole);
        log.debug("Đã assign role CUSTOMER cho user: {}", savedUser.getId());
        
        return savedUser;
    }
    
    /**
     * Generate unique userCode (random: 100000 - 999999)
     * <p>Tạo số ngẫu nhiên 6 chữ số và kiểm tra trùng trong database</p>
     */
    private Long generateUserCode() {
        java.util.Random random = new java.util.Random();
        Long userCode;
        int maxAttempts = 100; // Giới hạn số lần thử để tránh vòng lặp vô hạn
        int attempts = 0;
        
        do {
            // Random số từ 100000 đến 999999 (6 chữ số)
            userCode = 100000L + (long)(random.nextDouble() * 900000);
            attempts++;
            
            if (attempts >= maxAttempts) {
                log.error("Không thể tạo userCode unique sau {} lần thử", maxAttempts);
                throw new com.greenconnect.greenconnect_api.exceptions.BusinessException(
                    com.greenconnect.greenconnect_api.exceptions.ErrorCode.INTERNAL_SERVER_ERROR
                );
            }
        } while (userRepository.existsByUserCode(userCode)); // Kiểm tra trùng
        
        log.debug("Generated unique userCode: {}", userCode);
        return userCode;
    }
    
    /**
     * Load danh sách địa chỉ của user để trả về trong LoginResponse.
     */
    private List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> loadUserAddresses(java.util.UUID userId) {
        List<com.greenconnect.greenconnect_api.entities.Address> addresses = addressRepository.findByUser_Id(userId);
        
        return addresses.stream()
                .map(address -> com.greenconnect.greenconnect_api.dtos.response.AddressRespone.builder()
                        .id(address.getId())
                        .userId(address.getUserId())
                        .recipientName(address.getRecipientName())
                        .recipientPhone(address.getRecipientPhone())
                        .streetAddress(address.getStreetAddress())
                        .note(address.getNote())
                        // Vietnam Address (63 provinces)
                        .provinceCode63(address.getProvinceCode63())
                        .provinceName63(address.getProvinceName63())
                        .districtCode63(address.getDistrictCode63())
                        .districtName63(address.getDistrictName63())
                        .wardCode63(address.getWardCode63())
                        .wardName63(address.getWardName63())
                        // Vietnam Address (34 provinces - optional)
                        .provinceCode34(address.getProvinceCode34())
                        .provinceName34(address.getProvinceName34())
                        .wardCode34(address.getWardCode34())
                        .wardName34(address.getWardName34())
                        .addressType(address.getAddressType())
                        .isDefault(address.getIsDefault())
                        .createdAt(address.getCreatedAt())
                        .updatedAt(address.getUpdatedAt())
                        .build())
                .toList();
    }
    
    /**
     * Load danh sách sản phẩm yêu thích của user để trả về trong LoginResponse.
     */
    private List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> loadUserFavorites(java.util.UUID userId) {
        log.debug("Đang tải danh sách favorites cho user: {}", userId);
        
        List<com.greenconnect.greenconnect_api.entities.Favorite> favorites = favoriteRepository.findByUserIdAndIsActiveTrue(userId);
        
        return favorites.stream()
                .map(fav -> com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse.builder()
                        .id(fav.getId())
                        .productId(fav.getProduct().getId())
                        .build())
                .toList();
    }
    
    /**
     * Tạo LoginResponse từ User entity cho registration flow.
     * <p>Chỉ sinh token, KHÔNG lưu refresh token vào DB (vì chưa có device info).</p>
     * <p>Refresh token sẽ được lưu khi user đăng nhập lần đầu.</p>
     */
    private LoginResponse createLoginResponse(User user) {
        log.debug("Tạo LoginResponse cho user: {}", user.getId());
        
        // 1. Get roles and primary role for JWT token
        Set<Role> activeRoles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole == null) {
            log.error("User {} không có primary role", user.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 2. Tạo JWT tokens với multiple roles và primary role
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), activeRoles, primaryRole, user.getFullName());
        String refreshTokenValue = jwtUtils.generateRefreshToken(user.getId(), user.getEmail(), primaryRole.name(), user.getFullName());
        
        // 3. ❌ KHÔNG lưu refresh token vào DB ở registration
        // Refresh token sẽ được lưu khi user login (với device info, fcmToken, etc.)
        
        // 4. Tính token expires time
        long accessTokenExpiresIn = jwtUtils.getAccessTokenExpiration();
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60; // 30 days in seconds
        
        // 5. Load user addresses và favorites
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = loadUserAddresses(user.getId());
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(user.getId());
        
        // 6. Tạo LoginResponse
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenValue)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(java.time.LocalDateTime.now())
                .user(UserMapper.toUserResponse(user))
                .addresses(addresses)
                .favorites(favorites)
                .build();
        
        log.info("Tạo LoginResponse thành công cho user: {} với {} địa chỉ và {} favorites (refresh token chưa được lưu DB)", user.getEmail(), addresses.size(), favorites.size());
        return loginResponse;
    }
    
    /**
     * {@inheritDoc}
     */
   @Override
    @Transactional
    public LoginResponse loginUser(LoginRequest request) {
        log.info("🔐 [LOGIN] Bắt đầu đăng nhập user với email: {}", request.getEmail());
        log.info("🔐 [LOGIN] FCM Token: {}, Device Type: {}", 
                request.getFcmToken() != null ? request.getFcmToken().substring(0, Math.min(20, request.getFcmToken().length())) + "..." : "NULL",
                request.getDeviceType());
        
        String email = request.getEmail().trim();
        
        // 1. Tìm user theo email - sử dụng UserRepository
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Email không tồn tại: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Tài khoản bị vô hiệu hóa: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            log.warn("❌ Tài khoản chưa kích hoạt: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }
        
        // 3. Verify password với BCrypt encoder
        if (user.getPasswordHash() == null || !PasswordUtils.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("Sai mật khẩu cho email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 4. Get roles and primary role for JWT token
        Set<Role> activeRoles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole == null) {
            log.error("User {} không có primary role", user.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 5. Tạo JWT tokens với multiple roles và primary role
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), activeRoles, primaryRole, user.getFullName());
        String refreshTokenValue = jwtUtils.generateRefreshToken(user.getId(), user.getEmail(), primaryRole.name(), user.getFullName());
        
        // 6. Lưu refresh token với device info sử dụng RefreshTokenService
        String deviceInfo = request.getDeviceInfo() != null ? request.getDeviceInfo() : "Unknown Device";
        LocalDateTime expiryDate = java.time.LocalDateTime.now().plusDays(30); // 30 ngày
        
        refreshTokenService.createRefreshToken(user, refreshTokenValue, deviceInfo, expiryDate);
        
        // 7. Lưu hoặc cập nhật FCM token nếu được cung cấp
        log.info("🔔 [FCM] ========== KIỂM TRA FCM TOKEN TỪ LOGIN REQUEST ==========");
        log.info("🔔 [FCM] User đang login: {} ({})", user.getEmail(), user.getId());
        log.info("🔔 [FCM] FCM Token từ request: {}", request.getFcmToken() != null ? "CÓ (" + request.getFcmToken().length() + " ký tự)" : "NULL");
        if (request.getFcmToken() != null) {
            log.info("🔔 [FCM] FULL FCM TOKEN NHẬN ĐƯỢC: {}", request.getFcmToken());
        }
        log.info("🔔 [FCM] Device Type từ request: {}", request.getDeviceType());
        
        if (request.getFcmToken() != null && !request.getFcmToken().trim().isEmpty()) {
            log.info("🔔 [FCM] Bắt đầu lưu FCM token cho user: {}", user.getId());
            
            com.greenconnect.greenconnect_api.enums.DeviceType deviceTypeEnum = parseDeviceType(request.getDeviceType());
            log.info("🔔 [FCM] Device Type parsed: {}", deviceTypeEnum);
            saveOrUpdateFcmToken(user, request.getFcmToken(), deviceTypeEnum);
            log.info("🔔 [FCM] Đã lưu FCM token xong");
        } else {
            log.warn("⚠️ [FCM] FCM token NULL hoặc EMPTY - không lưu token");
        }
        log.info("🔔 [FCM] ========== KẾT THÚC KIỂM TRA ==========");
        
        // 8. Tính refresh token expires time (30 ngày)
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60; // 30 days in seconds
        
        // 9. Load user addresses và favorites
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = loadUserAddresses(user.getId());
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(user.getId());
        
        // 10. Tạo LoginResponse với đầy đủ thông tin token, addresses và favorites
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(jwtUtils.getAccessTokenExpiration())
                .refreshToken(refreshTokenValue)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(java.time.LocalDateTime.now())
                .user(UserMapper.toUserResponse(user))
                .addresses(addresses)
                .favorites(favorites)
                .build();
        
        log.info("Đăng nhập thành công cho user: {} với primary role: {}, {} địa chỉ và {} favorites", user.getEmail(), primaryRole, addresses.size(), favorites.size());
        return loginResponse;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        log.info("Bắt đầu làm mới token");
        
        String refreshTokenValue = request.getRefreshToken();
        
        // 1. Validate refresh token format và extract user info
        try {
            if (!jwtUtils.isTokenValid(refreshTokenValue)) {
                log.warn("Refresh token không hợp lệ");
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // ⭐ Refresh token đã hết hạn → 4424 REFRESH_TOKEN_EXPIRED
            log.warn("⏰ Refresh token đã hết hạn: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        
        // 2. Extract user info từ refresh token
        java.util.UUID userId = jwtUtils.getUserIdFromToken(refreshTokenValue);
        
        // 3. Tìm user trong database để đảm bảo vẫn active
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User không tồn tại với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 4. Kiểm tra refresh token sử dụng RefreshTokenService
        refreshTokenService.validateRefreshToken(user, refreshTokenValue);
        
        // 5. Get roles and primary role for JWT token
        Set<Role> activeRoles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole == null) {
            log.error("User {} không có primary role", user.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // ⚠️ 5.1. KIỂM TRA 2FA CHO NON-CUSTOMER USERS
        // Nếu user không phải CUSTOMER và chưa setup 2FA → từ chối refresh token
        if (primaryRole != Role.CUSTOMER) {
            if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
                log.warn("🚨 [REFRESH TOKEN] User '{}' không phải CUSTOMER nhưng chưa setup 2FA. Từ chối refresh token.", user.getEmail());
                throw new BusinessException(ErrorCode.TWO_FACTOR_REQUIRED);
            }
            log.info("✅ [REFRESH TOKEN] User '{}' đã setup 2FA. Cho phép refresh token.", user.getEmail());
        }
        
        // 6. Tạo ACCESS TOKEN MỚI (chỉ access token, KHÔNG tạo refresh token mới)
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), 
                                                             activeRoles, primaryRole, user.getFullName());
        
        // 7. KHÔNG tạo refresh token mới - giữ nguyên refresh token cũ
        // Refresh token chỉ được thay thế khi login lại
        
        // 8. Tạo LoginResponse với ACCESS TOKEN mới và REFRESH TOKEN CŨ
        long accessTokenExpiresIn = jwtUtils.getAccessTokenExpiration();
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60; // 30 days in seconds
        
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)              // ✅ Access Token MỚI  
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenValue)          // ✅ Refresh Token CŨ (không đổi)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(java.time.LocalDateTime.now())
                .user(UserMapper.toUserResponse(user))
                .build();
        
        log.info("Làm mới token thành công cho user: {}", user.getEmail());
        return loginResponse;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse loginFirebaseUser(LoginFirebaseRequest request) {
        log.info("🔐 [FIREBASE LOGIN] Bắt đầu đăng nhập Firebase user với email: {}", request.getEmail());
        log.info("🔐 [FIREBASE LOGIN] FCM Token: {}, Device Type: {}", 
                request.getFcmToken() != null ? request.getFcmToken().substring(0, Math.min(20, request.getFcmToken().length())) + "..." : "NULL",
                request.getDeviceType());
        
        String email = request.getEmail().toLowerCase().trim();
        String providerId = request.getProviderId();
        
        // 1. Validate Firebase data
        if (email == null || email.trim().isEmpty()) {
            log.warn("Email không được để trống");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        if (providerId == null || providerId.trim().isEmpty()) {
            log.warn("Provider ID không được để trống");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 2. Tìm user theo email và provider GOOGLE
        User user = userRepository.findByEmailIgnoreCaseAndProvider(email, Provider.GOOGLE)
                .orElseThrow(() -> {
                    log.warn("User Firebase không tồn tại với email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Tài khoản bị vô hiệu hóa: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            log.warn("❌ Tài khoản chưa kích hoạt: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }
        
        // 4. Verify providerId khớp với stored hash
        if (user.getProviderId() != null && 
            !PasswordUtils.verifyPassword(providerId, user.getProviderId())) {
            log.warn("Provider ID không khớp cho Firebase user: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 5. Get roles and primary role for JWT token
        Set<Role> activeRoles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole == null) {
            log.error("Firebase user {} không có primary role", user.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 6. Tạo JWT access token với multiple roles và refresh token với primary role
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), 
                                                          activeRoles, primaryRole, user.getFullName());
        String refreshTokenValue = jwtUtils.generateRefreshToken(user.getId(), user.getEmail(), 
                                                                primaryRole.name(), user.getFullName());
        
        // 7. Lưu refresh token với device info sử dụng RefreshTokenService (chỉ lúc login)
        String deviceInfo = request.getDeviceInfo() != null ? request.getDeviceInfo() : "Firebase Login Device";
        LocalDateTime expiryDate = java.time.LocalDateTime.now().plusDays(7); // 7 ngày cho Firebase
        
        refreshTokenService.createRefreshToken(user, refreshTokenValue, deviceInfo, expiryDate);
        
        // 8. Lưu hoặc cập nhật FCM token nếu được cung cấp
        log.info("🔔 [FCM FIREBASE] Kiểm tra FCM token: {}", request.getFcmToken() != null ? "CÓ" : "NULL");
        if (request.getFcmToken() != null && !request.getFcmToken().trim().isEmpty()) {
            log.info("🔔 [FCM FIREBASE] Bắt đầu lưu FCM token cho user: {}", user.getId());
            
            com.greenconnect.greenconnect_api.enums.DeviceType deviceTypeEnum = parseDeviceType(request.getDeviceType());
            log.info("🔔 [FCM FIREBASE] Device Type parsed: {}", deviceTypeEnum);
            saveOrUpdateFcmToken(user, request.getFcmToken(), deviceTypeEnum);
            log.info("🔔 [FCM FIREBASE] Đã lưu FCM token xong");
        } else {
            log.warn("⚠️ [FCM FIREBASE] FCM token NULL hoặc EMPTY - không lưu token");
        }
        
        // 9. Tính token expires time
        long accessTokenExpiresIn = jwtUtils.getAccessTokenExpiration();
        long refreshTokenExpiresIn = 7 * 24 * 60 * 60; // 7 days in seconds for Firebase
        
        // 10. Load user addresses và favorites
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = loadUserAddresses(user.getId());
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(user.getId());
        
        // 11. Tạo LoginResponse với addresses và favorites
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenValue)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(java.time.LocalDateTime.now())
                .user(UserMapper.toUserResponse(user))
                .addresses(addresses)
                .favorites(favorites)
                .build();
        
        log.info("Đăng nhập Firebase thành công cho user: {} với provider: GOOGLE, {} địa chỉ và {} favorites", user.getEmail(), addresses.size(), favorites.size());
        return loginResponse;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateUser(java.util.UUID userId, UpdateUserRequest request) {
        log.info("Bắt đầu cập nhật thông tin user với ID: {}", userId);
        
        // 1. Tìm user cần update
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy user với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Update các field khác null
        boolean hasChanges = false;
        
        // ========== PROFILE INFORMATION ==========
        
        // 2.1. Update fullName
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
            hasChanges = true;
            log.debug("Updated fullName for user: {}", userId);
        }
        
        // 2.2. Update phoneNumber
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
            hasChanges = true;
            log.debug("Updated phoneNumber for user: {}", userId);
        }
        
        // 2.3. Update avatarUrl
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
            hasChanges = true;
            log.debug("Updated avatarUrl for user: {}", userId);
        }
        
        // ========== PASSWORD MANAGEMENT ==========
        
        // 2.4. Update password (hash before save)
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            // Kiểm tra oldPassword có được cung cấp không
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                log.warn("Old password không được cung cấp khi đổi password cho user: {}", userId);
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            
            // Verify old password với stored password hash
            if (user.getPasswordHash() == null || !PasswordUtils.verifyPassword(request.getOldPassword(), user.getPasswordHash())) {
                log.warn("Old password không đúng cho user: {}", userId);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            
            // Old password đúng, proceed to update new password
            String hashedPassword = PasswordUtils.hashPassword(request.getPassword());
            user.setPasswordHash(hashedPassword);
            hasChanges = true;
            log.debug("Updated password for user: {}", userId);
        }
        
        // ========== BANK INFORMATION ==========
        
        // 2.5. Update nameAccountBank
        if (request.getNameAccountBank() != null && !request.getNameAccountBank().trim().isEmpty()) {
            user.setNameAccountBank(request.getNameAccountBank().trim());
            hasChanges = true;
            log.debug("Updated nameAccountBank for user: {}", userId);
        }
        
        // 2.6. Update nameBank
        if (request.getNameBank() != null && !request.getNameBank().trim().isEmpty()) {
            user.setNameBank(request.getNameBank().trim());
            hasChanges = true;
            log.debug("Updated nameBank for user: {}", userId);
        }
        
        // 2.7. Update accountNumberBank
        if (request.getAccountNumberBank() != null) {
            user.setAccountNumberBank(request.getAccountNumberBank());
            hasChanges = true;
            log.debug("Updated accountNumberBank for user: {}", userId);
        }
        
        // 2.8. Update nameBankCode
        if (request.getNameBankCode() != null && !request.getNameBankCode().trim().isEmpty()) {
            user.setNameBankCode(request.getNameBankCode().trim());
            hasChanges = true;
            log.debug("Updated nameBankCode for user: {}", userId);
        }
        
        // ========== PAYMENT PREFERENCES ==========
        
        // 2.9. Update preferredPaymentMethod
        if (request.getPreferredPaymentMethod() != null) {
            user.setPreferredPaymentMethod(request.getPreferredPaymentMethod());
            hasChanges = true;
            log.debug("Updated preferredPaymentMethod for user: {} to {}", userId, request.getPreferredPaymentMethod());
        }
        
        // ========== ADMIN ONLY FIELDS ==========
        
        // 2.9. Update loyaltyPoints
        if (request.getLoyaltyPoints() != null) {
            user.setLoyaltyPoints(request.getLoyaltyPoints());
            hasChanges = true;
            log.debug("Updated loyaltyPoints for user: {}", userId);
        }
        
        // 2.10. Update status
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            hasChanges = true;
            log.debug("Updated status for user: {}", userId);
        }
        
        // 2.11. Update totalPaymentAmount
        if (request.getTotalPaymentAmount() != null) {
            user.setTotalPaymentAmount(request.getTotalPaymentAmount());
            hasChanges = true;
            log.debug("Updated totalPaymentAmount for user: {}", userId);
        }
        
        // 2.12. Update roles (ADMIN ONLY)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            log.info("🔄 [UPDATE ROLES] Bắt đầu cập nhật roles cho user '{}'", userId);
            
            // Delete old roles
            userRoleRepository.deleteByUser(user);
            userRoleRepository.flush();
            
            // Add new roles
            for (Role role : request.getRoles()) {
                UserRole userRole = UserRole.builder()
                        .user(user)
                        .role(role)
                        .active(true)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(userRole);
                log.info("✅ Đã thêm role {} cho user '{}'", role, userId);
            }
            
            hasChanges = true;
        }
        
        // 3. Save changes
        if (hasChanges) {
            User savedUser = userRepository.save(user);
            log.info("Cập nhật thông tin user thành công: {}", userId);
            
            // Refresh từ DB nếu có update roles để lấy UserRole relationships mới
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                userRepository.flush();
                User refreshedUser = userRepository.findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                
                log.info("🔄 [REFRESH USER] Đã refresh user từ DB, roles hiện tại: {}", 
                        refreshedUser.getActiveRoles());
                
                return UserMapper.toUserResponse(refreshedUser);
            }
            
            return UserMapper.toUserResponse(savedUser);
        } else {
            log.info("Không có thay đổi nào cho user: {}", userId);
            return UserMapper.toUserResponse(user);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserResponse> getAllUsers() {
        log.info("Lấy danh sách tất cả users");
        
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateUserByEmail(String email, UpdateUserRequest request) {
        log.info("Bắt đầu cập nhật thông tin user với email: {}", email);
        
        // 1. Tìm user theo email
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy user với email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        boolean hasChanges = false;
        
        // ========== PROFILE INFORMATION ==========
        
        // 2.1. Update fullName
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
            hasChanges = true;
            log.debug("Updated fullName for user: {}", email);
        }
        
        // 2.2. Update phoneNumber
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
            hasChanges = true;
            log.debug("Updated phoneNumber for user: {}", email);
        }
        
        // 2.3. Update avatarUrl
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().trim().isEmpty()) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
            hasChanges = true;
            log.debug("Updated avatarUrl for user: {}", email);
        }
        
        // ========== PASSWORD MANAGEMENT ==========
        
        // 2.4. Update password (hash before save)
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            // Kiểm tra oldPassword có được cung cấp không
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                log.warn("Old password không được cung cấp khi đổi password cho user: {}", email);
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            
            // Verify old password với stored password hash
            if (user.getPasswordHash() == null || !PasswordUtils.verifyPassword(request.getOldPassword(), user.getPasswordHash())) {
                log.warn("Old password không đúng cho user: {}", email);
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            
            String hashedPassword = PasswordUtils.hashPassword(request.getPassword());
            user.setPasswordHash(hashedPassword);
            hasChanges = true;
            log.debug("Updated password for user: {}", email);
        }
        
        // ========== BANK INFORMATION ==========
        
        // 2.5. Update nameAccountBank
        if (request.getNameAccountBank() != null && !request.getNameAccountBank().trim().isEmpty()) {
            user.setNameAccountBank(request.getNameAccountBank().trim());
            hasChanges = true;
            log.debug("Updated nameAccountBank for user: {}", email);
        }
        
        // 2.6. Update nameBank
        if (request.getNameBank() != null && !request.getNameBank().trim().isEmpty()) {
            user.setNameBank(request.getNameBank().trim());
            hasChanges = true;
            log.debug("Updated nameBank for user: {}", email);
        }
        
        // 2.7. Update accountNumberBank
        if (request.getAccountNumberBank() != null) {
            user.setAccountNumberBank(request.getAccountNumberBank());
            hasChanges = true;
            log.debug("Updated accountNumberBank for user: {}", email);
        }
        
        // 2.8. Update nameBankCode
        if (request.getNameBankCode() != null && !request.getNameBankCode().trim().isEmpty()) {
            user.setNameBankCode(request.getNameBankCode().trim());
            hasChanges = true;
            log.debug("Updated nameBankCode for user: {}", email);
        }
        
        // ========== PAYMENT PREFERENCES ==========
        
        // 2.9. Update preferredPaymentMethod
        if (request.getPreferredPaymentMethod() != null) {
            user.setPreferredPaymentMethod(request.getPreferredPaymentMethod());
            hasChanges = true;
            log.debug("Updated preferredPaymentMethod for user: {} to {}", email, request.getPreferredPaymentMethod());
        }
        
        // ========== ADMIN ONLY FIELDS ==========
        
        // 2.10. Update loyaltyPoints
        if (request.getLoyaltyPoints() != null) {
            user.setLoyaltyPoints(request.getLoyaltyPoints());
            hasChanges = true;
            log.debug("Updated loyaltyPoints for user: {}", email);
        }
        
        // 2.10. Update status
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            hasChanges = true;
            log.debug("Updated status for user: {}", email);
        }
        
        // 2.11. Update totalPaymentAmount
        if (request.getTotalPaymentAmount() != null) {
            user.setTotalPaymentAmount(request.getTotalPaymentAmount());
            hasChanges = true;
            log.debug("Updated totalPaymentAmount for user: {}", email);
        }
        
        // 2.12. Update roles (ADMIN ONLY)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            log.info("🔄 [UPDATE ROLES] Bắt đầu cập nhật roles cho user '{}'", email);
            
            // Delete old roles
            userRoleRepository.deleteByUser(user);
            userRoleRepository.flush();
            
            // Add new roles
            for (Role role : request.getRoles()) {
                UserRole userRole = UserRole.builder()
                        .user(user)
                        .role(role)
                        .active(true)
                        .assignedAt(LocalDateTime.now())
                        .build();
                userRoleRepository.save(userRole);
                log.info("✅ Đã thêm role {} cho user '{}'", role, email);
            }
            
            hasChanges = true;
        }
        
        // 3. Save changes
        if (hasChanges) {
            User savedUser = userRepository.save(user);
            log.info("💾 [SAVE USER] Đã lưu user '{}' vào database", email);
            
            // ⭐⭐⭐ QUAN TRỌNG: REFRESH USER TỪ DB ĐỂ LẤY CÁC USERROLE MỚI ⭐⭐⭐
            // Nếu có update roles, cần query lại để lấy UserRole relationships mới
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                userRepository.flush(); // Đảm bảo tất cả changes đã được persist
                
                User refreshedUser = userRepository.findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                
                log.info("🔄 [REFRESH USER] Đã refresh user từ DB, roles hiện tại: {}", 
                        refreshedUser.getActiveRoles());
                
                return UserMapper.toUserResponse(refreshedUser);
            }
            
            log.info("Cập nhật thông tin user thành công: {}", email);
            return UserMapper.toUserResponse(savedUser);
        } else {
            log.info("Không có thay đổi nào cho user: {}", email);
            return UserMapper.toUserResponse(user);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(java.util.UUID userId) {
        log.info("📋 [GET USER BY ID] Đang lấy thông tin user với ID: {}", userId);
        
        // 1. Tìm user trong database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("❌ [GET USER BY ID] Không tìm thấy user với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        log.info("✅ [GET USER BY ID] Đã tìm thấy user '{}' (email: {}) với roles: {}", 
                user.getFullName(), user.getEmail(), 
                user.getUserRoles().stream().map(ur -> ur.getRole().name()).toList());
        
        // 2. Lấy địa chỉ mặc định của user (nếu có)
        com.greenconnect.greenconnect_api.entities.Address defaultAddress = 
            addressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
        
        if (defaultAddress != null) {
            log.debug("📍 [GET USER BY ID] Tìm thấy địa chỉ mặc định: {} - {}", 
                    defaultAddress.getRecipientName(), defaultAddress.getRecipientPhone());
        } else {
            log.debug("📍 [GET USER BY ID] User chưa có địa chỉ mặc định");
        }
        
        // 3. Convert sang UserResponse với defaultAddress
        return UserMapper.toUserResponseWithAddress(user, defaultAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("📋 [GET USER BY EMAIL] Đang lấy thông tin user với email: {}", email);
        
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("❌ [GET USER BY EMAIL] Không tìm thấy user với email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        log.info("✅ [GET USER BY EMAIL] Đã tìm thấy user '{}' với roles: {}", 
                email, user.getUserRoles().stream().map(ur -> ur.getRole().name()).toList());
        
        return UserMapper.toUserResponse(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.UserProfileResponse getUserProfile(java.util.UUID userId) {
        log.info("📋 [GET USER PROFILE] Lấy thông tin đầy đủ của user: {}", userId);
        
        // 1. Tìm user trong database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        log.debug("Tìm thấy user với email: {}", user.getEmail());
        
        // 2. Convert User entity to UserResponse
        UserResponse userResponse = com.greenconnect.greenconnect_api.mappers.UserMapper.toUserResponse(user);
        
        // 3. Load danh sách địa chỉ của user
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = loadUserAddresses(userId);
        log.debug("Đã load {} địa chỉ cho user: {}", addresses.size(), userId);
        
        // 4. Load danh sách sản phẩm yêu thích của user
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(userId);
        log.debug("Đã load {} sản phẩm yêu thích cho user: {}", favorites.size(), userId);
        
        // 5. Tạo UserProfileResponse (giống LoginResponse nhưng không có tokens)
        com.greenconnect.greenconnect_api.dtos.response.UserProfileResponse response = 
            com.greenconnect.greenconnect_api.dtos.response.UserProfileResponse.builder()
                .userId(userId)
                .user(userResponse)
                .addresses(addresses)
                .favorites(favorites)
                .retrievedAt(LocalDateTime.now())
                .build();
        
        log.info("✅ [GET USER PROFILE] Đã lấy thông tin đầy đủ cho user: {} (email: {})", userId, user.getEmail());
        return response;
    }

    // ========== LOGOUT & TOKEN MANAGEMENT IMPLEMENTATION ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean logoutCurrentDevice(java.util.UUID userId, String refreshToken, String fcmToken) {
        log.info("🚪 [LOGOUT] User {} đang logout từ thiết bị hiện tại", userId);
        log.info("🚪 [LOGOUT] FCM Token: {}", fcmToken != null ? fcmToken.substring(0, Math.min(20, fcmToken.length())) + "..." : "NULL");
        log.info("Logout current device cho user: {}", userId);
        
        // 1. Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User không tồn tại với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Revoke refresh token cụ thể
        boolean success = refreshTokenService.revokeRefreshToken(user, refreshToken);
        
        // 3. Xóa FCM token nếu có (chỉ xóa token thuộc đúng user này)
        if (fcmToken != null && !fcmToken.trim().isEmpty()) {
            try {
                log.info("🔔 [LOGOUT] Đang xóa FCM token của user {} khỏi database...", userId);
                int deletedCount = fcmTokenRepository.deleteByUserIdAndToken(userId, fcmToken.trim());
                if (deletedCount > 0) {
                    log.info("✅ [LOGOUT] Đã xóa FCM token thành công khỏi database");
                } else {
                    log.warn("⚠️ [LOGOUT] FCM token không tồn tại hoặc không thuộc user này");
                }
            } catch (Exception e) {
                log.error("❌ [LOGOUT] Lỗi khi xóa FCM token: {}", e.getMessage());
                // Không throw exception - FCM token deletion là optional
            }
        } else {
            log.info("ℹ️ [LOGOUT] Không có FCM token để xóa");
        }
        
        if (success) {
            log.info("✅ [LOGOUT] Logout thành công từ device cho user: {}", user.getEmail());
        } else {
            log.warn("❌ [LOGOUT] Không thể logout - refresh token không hợp lệ cho user: {}", user.getEmail());
        }
        
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int logoutAllDevices(java.util.UUID userId) {
        log.info("🚪 [LOGOUT ALL] Logout all devices cho user: {}", userId);
        
        // 1. Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User không tồn tại với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Revoke tất cả refresh tokens
        int loggedOutDevices = refreshTokenService.revokeAllRefreshTokens(user);
        
        // 3. Xóa tất cả FCM tokens của user
        try {
            log.info("🔔 [LOGOUT ALL] Đang xóa tất cả FCM tokens của user...");
            int deactivatedTokens = fcmTokenRepository.deactivateAllTokensByUserId(userId);
            log.info("✅ [LOGOUT ALL] Đã xóa {} FCM tokens", deactivatedTokens);
        } catch (Exception e) {
            log.error("❌ [LOGOUT ALL] Lỗi khi xóa FCM tokens: {}", e.getMessage());
            // Không throw exception - FCM token deletion là optional
        }
        
        log.info("✅ [LOGOUT ALL] Đã logout {} devices cho user: {}", loggedOutDevices, user.getEmail());
        return loggedOutDevices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Object> getActiveDevices(java.util.UUID userId) {
        log.info("Lấy danh sách active devices cho user: {}", userId);
        
        // 1. Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User không tồn tại với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Lấy active refresh tokens
        return refreshTokenService.getActiveRefreshTokens(user)
                .stream()
                .map(token -> {
                    var deviceInfo = new java.util.HashMap<String, Object>();
                    deviceInfo.put("id", token.getId());
                    deviceInfo.put("deviceInfo", token.getDeviceInfo());
                    deviceInfo.put("createdAt", token.getCreatedAt());
                    deviceInfo.put("expiryDate", token.getExpiryDate());
                    return deviceInfo;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Đăng ký hoặc cập nhật UserDevice cho push notification.
     * Nếu device với fcmToken đã tồn tại, cập nhật; nếu không thì tạo mới.
     */
    private void registerOrUpdateUserDevice(User user, String fcmToken, String deviceType, String deviceInfo) {
        try {
            // 1. Kiểm tra device với FCM token này đã tồn tại chưa
            var existingDevice = userDeviceRepository.findByFcmToken(fcmToken);
            
            if (existingDevice.isPresent()) {
                updateExistingDevice(existingDevice.get(), user.getId());
            } else {
                createNewDevice(user, fcmToken, deviceType, deviceInfo);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi đăng ký UserDevice cho user: {}, error: {}", user.getId(), e.getMessage());
            // Không throw exception - device registration là optional, không nên break login flow
        }
    }
    
    /**
     * Cập nhật device hiện có (set active và update timestamp).
     */
    private void updateExistingDevice(UserDevice device, java.util.UUID userId) {
        device.setIsActive(true);
        device.setUpdatedAt(LocalDateTime.now());
        userDeviceRepository.save(device);
        log.info("Cập nhật UserDevice hiện có cho user: {} với token: {}", userId, 
                device.getFcmToken().substring(0, 20) + "...");
    }
    
    /**
     * Tạo device mới.
     */
    private void createNewDevice(User user, String fcmToken, String deviceType, String deviceInfo) {
        UserDevice newDevice = UserDevice.builder()
                .user(user)
                .fcmToken(fcmToken)
                .deviceType(deviceType != null ? com.greenconnect.greenconnect_api.enums.DeviceType.valueOf(deviceType.toUpperCase()) : com.greenconnect.greenconnect_api.enums.DeviceType.WEB)
                .deviceName(extractDeviceName(deviceInfo))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userDeviceRepository.save(newDevice);
        log.info("Tạo UserDevice mới cho user: {} với type: {}", user.getId(), newDevice.getDeviceType());
    }
    
    /**
     * Trích xuất device name từ deviceInfo string.
     * Ví dụ: "iPhone 12 Pro" từ "iPhone 12 Pro/iOS 15.0"
     */
    private String extractDeviceName(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.trim().isEmpty()) {
            return "Unknown Device";
        }
        
        // Lấy phần đầu tiên trước dấu "/"
        if (deviceInfo.contains("/")) {
            return deviceInfo.split("/")[0].trim();
        }
        
        return deviceInfo.length() > 100 ? deviceInfo.substring(0, 100) : deviceInfo;
    }
    
    /**
     * Parse DeviceType từ String sang enum.
     * <p>Nếu không parse được, trả về WEB làm default.</p>
     */
    private com.greenconnect.greenconnect_api.enums.DeviceType parseDeviceType(String deviceTypeStr) {
        if (deviceTypeStr == null || deviceTypeStr.trim().isEmpty()) {
            return com.greenconnect.greenconnect_api.enums.DeviceType.WEB;
        }
        
        try {
            return com.greenconnect.greenconnect_api.enums.DeviceType.valueOf(deviceTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Không parse được DeviceType từ string: {}, sử dụng WEB làm default", deviceTypeStr);
            return com.greenconnect.greenconnect_api.enums.DeviceType.WEB;
        }
    }
    
    /**
     * Lưu hoặc cập nhật FCM token cho user.
     * <p>MÔ HÌNH ĐA THIẾT BỊ (Multi-Device): User có thể đăng nhập trên nhiều thiết bị cùng loại</p>
     * <p>Logic:</p>
     * <ol>
     *   <li>Bước 1: Kiểm tra token string này đã tồn tại trong DB chưa</li>
     *   <li>Bước 2: Nếu token đã tồn tại VÀ thuộc về user này → cập nhật lastUpdated</li>
     *   <li>Bước 3: Nếu token đã tồn tại NHƯNG thuộc user khác → XÓA record cũ, tạo mới cho user này</li>
     *   <li>Bước 4: Nếu token chưa tồn tại → INSERT mới (KHÔNG xóa token cũ của user)</li>
     * </ol>
     * <p>Nguyên tắc: 1 token string = UNIQUE, nhưng 1 user có thể có NHIỀU tokens</p>
     */
    private void saveOrUpdateFcmToken(User user, String fcmToken, com.greenconnect.greenconnect_api.enums.DeviceType deviceType) {
        log.info("💾 [SAVE FCM TOKEN] ==== BẮT ĐẦU ==== ");
        log.info("💾 [SAVE FCM TOKEN] User ID: {}, Email: {}", user.getId(), user.getEmail());
        log.info("💾 [SAVE FCM TOKEN] Token đầu: {}", 
                fcmToken != null ? fcmToken.substring(0, Math.min(30, fcmToken.length())) : "NULL");
        log.info("💾 [SAVE FCM TOKEN] Token cuối: {}", 
                fcmToken != null ? fcmToken.substring(Math.max(0, fcmToken.length() - 20)) : "NULL");
        log.info("💾 [SAVE FCM TOKEN] Device Type: {}", deviceType);
        
        try {
            if (fcmToken == null || fcmToken.trim().isEmpty()) {
                log.warn("⚠️ [SAVE FCM TOKEN] FCM token null hoặc empty, bỏ qua lưu token");
                return;
            }
            
            String trimmedToken = fcmToken.trim();
            com.greenconnect.greenconnect_api.enums.DeviceType finalDeviceType = 
                    deviceType != null ? deviceType : com.greenconnect.greenconnect_api.enums.DeviceType.WEB;
            
            // Bước 1: Kiểm tra token string này đã tồn tại trong DB chưa (UNIQUE by token)
            log.info("💾 [SAVE FCM TOKEN] Bước 1: Kiểm tra token đã tồn tại chưa...");
            java.util.Optional<com.greenconnect.greenconnect_api.entities.FcmToken> existingToken = 
                    fcmTokenRepository.findByToken(trimmedToken);
            
            if (existingToken.isPresent()) {
                com.greenconnect.greenconnect_api.entities.FcmToken token = existingToken.get();
                java.util.UUID existingUserId = token.getUser().getId();
                
                if (existingUserId.equals(user.getId())) {
                    // Bước 2: Token đã thuộc về user này → chỉ cập nhật timestamp và device type
                    log.info("💾 [SAVE FCM TOKEN] ✅ Token ĐÃ thuộc về user này - Cập nhật timestamp");
                    token.setDeviceType(finalDeviceType);
                    token.setIsActive(true);
                    token.setLastUpdated(LocalDateTime.now());
                    fcmTokenRepository.save(token);
                    log.info("✅ [SAVE FCM TOKEN] CẬP NHẬT THÀNH CÔNG - ID: {}", token.getId());
                } else {
                    // Bước 3: Token thuộc về USER KHÁC → "Giành quyền sở hữu"
                    // Xóa record cũ của user khác, tạo mới cho user hiện tại
                    log.warn("⚠️ [SAVE FCM TOKEN] Token đang thuộc về user KHÁC: {} → Giành quyền cho user: {}", 
                            existingUserId, user.getId());
                    
                    // Xóa token cũ của user khác
                    fcmTokenRepository.delete(token);
                    fcmTokenRepository.flush(); // Đảm bảo xóa xong trước khi tạo mới
                    log.info("💾 [SAVE FCM TOKEN] Đã XÓA token cũ của user {}", existingUserId);
                    
                    // Tạo token mới cho user hiện tại
                    com.greenconnect.greenconnect_api.entities.FcmToken newToken = 
                            com.greenconnect.greenconnect_api.entities.FcmToken.builder()
                                    .user(user)
                                    .token(trimmedToken)
                                    .deviceType(finalDeviceType)
                                    .isActive(true)
                                    .lastUpdated(LocalDateTime.now())
                                    .build();
                    com.greenconnect.greenconnect_api.entities.FcmToken savedToken = fcmTokenRepository.save(newToken);
                    log.info("✅ [SAVE FCM TOKEN] TẠO MỚI THÀNH CÔNG - ID: {}, User: {}", 
                            savedToken.getId(), savedToken.getUser().getId());
                }
            } else {
                // Bước 4: Token CHƯA tồn tại trong DB → INSERT mới
                // KHÔNG XÓA token cũ của user (cho phép đa thiết bị)
                log.info("💾 [SAVE FCM TOKEN] ⭐ Token CHƯA tồn tại - Tạo mới (Multi-Device)");
                
                com.greenconnect.greenconnect_api.entities.FcmToken newToken = 
                        com.greenconnect.greenconnect_api.entities.FcmToken.builder()
                                .user(user)
                                .token(trimmedToken)
                                .deviceType(finalDeviceType)
                                .isActive(true)
                                .lastUpdated(LocalDateTime.now())
                                .build();
                
                com.greenconnect.greenconnect_api.entities.FcmToken savedToken = fcmTokenRepository.save(newToken);
                log.info("✅ [SAVE FCM TOKEN] TẠO MỚI THÀNH CÔNG - ID: {}, User: {}, DeviceType: {}", 
                        savedToken.getId(), savedToken.getUser().getId(), savedToken.getDeviceType());
            }
            log.info("💾 [SAVE FCM TOKEN] ==== KẾT THÚC THÀNH CÔNG ==== \n");
        } catch (Exception e) {
            log.error("❌ [SAVE FCM TOKEN] ==== LỖI ==== ");
            log.error("❌ [SAVE FCM TOKEN] User ID: {}, Error: {}", user.getId(), e.getMessage());
            log.error("❌ [SAVE FCM TOKEN] Stack trace: ", e);
            log.error("❌ [SAVE FCM TOKEN] ==== KẾT THÚC VỚI LỖI ==== \n");
        }
    }

    // ========== 2FA (TOTP) AUTHENTICATION IMPLEMENTATION ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginInitialResponse loginUserWithTotp(LoginRequest request) {
        log.info("🔐 [LOGIN WITH TOTP] Bắt đầu đăng nhập với email: {}", request.getEmail());
        
        String email = request.getEmail().trim();
        
        // 1. Tìm user và verify password
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("❌ Email không tồn tại: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Tài khoản bị vô hiệu hóa: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            log.warn("❌ Tài khoản chưa kích hoạt: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }
        
        // 3. Verify password
        if (user.getPasswordHash() == null || !PasswordUtils.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            log.warn("❌ Sai mật khẩu cho email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 4. Kiểm tra 2FA status
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            // Chưa kích hoạt 2FA → tạo secret key và trả về QR code
            return generateQrCodeResponse(user);
        } else {
            // Đã kích hoạt 2FA → yêu cầu nhập OTP
            return requireOtpResponse(user);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginInitialResponse loginFirebaseUserWithTotp(LoginFirebaseRequest request) {
        log.info("🔐 [FIREBASE LOGIN WITH TOTP] Bắt đầu đăng nhập Firebase với email: {}", request.getEmail());
        
        String email = request.getEmail().toLowerCase().trim();
        String providerId = request.getProviderId();
        
        // 1. Validate Firebase data
        if (email == null || email.trim().isEmpty()) {
            log.warn("❌ Email không được để trống");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        if (providerId == null || providerId.trim().isEmpty()) {
            log.warn("❌ Provider ID không được để trống");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 2. Tìm user theo email và provider GOOGLE
        User user = userRepository.findByEmailIgnoreCaseAndProvider(email, Provider.GOOGLE)
                .orElseThrow(() -> {
                    log.warn("❌ User Firebase không tồn tại với email: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Tài khoản bị vô hiệu hóa: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            log.warn("❌ Tài khoản chưa kích hoạt: {}", email);
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }
        
        // 4. Verify providerId
        if (user.getProviderId() != null && 
            !PasswordUtils.verifyPassword(providerId, user.getProviderId())) {
            log.warn("❌ Provider ID không khớp cho Firebase user: {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 5. Kiểm tra 2FA status
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            // Chưa kích hoạt 2FA → tạo secret key và trả về QR code
            return generateQrCodeResponse(user);
        } else {
            // Đã kích hoạt 2FA → yêu cầu nhập OTP
            return requireOtpResponse(user);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        log.info("🔐 [VERIFY OTP] Đang xác thực OTP cho user: {}", request.getUserId());
        
        // 1. Tìm user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.warn("❌ User không tồn tại với ID: {}", request.getUserId());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("❌ Tài khoản bị vô hiệu hóa: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            log.warn("❌ Tài khoản chưa kích hoạt: {}", user.getEmail());
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }
        
        // 3. Kiểm tra secret key tồn tại
        if (user.getSecretKey2FA() == null || user.getSecretKey2FA().trim().isEmpty()) {
            log.warn("❌ User chưa có secret key 2FA");
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // 3. Verify OTP
        boolean isValid = TotpUtils.verifyOtp(request.getOtpCode(), user.getSecretKey2FA());
        
        if (!isValid) {
            log.warn("❌ OTP không hợp lệ cho user: {}", user.getEmail());
            // Trả về error với code 4890 theo yêu cầu
            throw new BusinessException(ErrorCode.INVALID_OTP);
        }
        
        // 4. OTP hợp lệ → kích hoạt 2FA nếu chưa kích hoạt
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            user.setTotpEnabled(true);
            userRepository.save(user);
            log.info("✅ Đã kích hoạt 2FA cho user: {}", user.getEmail());
        }
        
        // 4.1. ⚠️ KIỂM TRA BẮT BUỘC 2FA CHO NON-CUSTOMER USERS
        // Nếu user không phải CUSTOMER và chưa có 2FA → không cho đăng nhập
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole != null && primaryRole != Role.CUSTOMER) {
            if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
                log.warn("🚨 [VERIFY OTP] User '{}' không phải CUSTOMER nhưng chưa setup 2FA. Từ chối đăng nhập.", user.getEmail());
                throw new BusinessException(ErrorCode.TWO_FACTOR_REQUIRED);
            }
            log.info("✅ [VERIFY OTP] User '{}' (role: {}) đã setup 2FA. Cho phép đăng nhập.", user.getEmail(), primaryRole);
        }
        
        // 5. Tạo tokens và login
        return createLoginResponseWithDevice(user, request.getDeviceInfo(), 
                                            request.getFcmToken(), request.getDeviceType());
    }

    // ========== HELPER METHODS FOR TOTP ==========

    /**
     * Tạo response với QR code cho user chưa kích hoạt 2FA.
     */
    private LoginInitialResponse generateQrCodeResponse(User user) {
        log.info("📱 [GENERATE QR] Tạo QR code cho user: {}", user.getEmail());
        
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
     * Tạo response yêu cầu nhập OTP cho user đã kích hoạt 2FA.
     */
    private LoginInitialResponse requireOtpResponse(User user) {
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

    /**
     * Tạo LoginResponse với device info sau khi verify OTP thành công.
     */
    private LoginResponse createLoginResponseWithDevice(User user, String deviceInfo, 
                                                        String fcmToken, String deviceType) {
        log.debug("🔑 [CREATE TOKENS] Tạo tokens cho user: {}", user.getId());
        
        // 1. Get roles
        Set<Role> activeRoles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        if (primaryRole == null) {
            log.error("❌ User {} không có primary role", user.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 2. Tạo JWT tokens
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), 
                                                          activeRoles, primaryRole, user.getFullName());
        String refreshTokenValue = jwtUtils.generateRefreshToken(user.getId(), user.getEmail(), 
                                                                primaryRole.name(), user.getFullName());
        
        // 3. Lưu refresh token
        String deviceInfoStr = deviceInfo != null ? deviceInfo : "Unknown Device";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
        refreshTokenService.createRefreshToken(user, refreshTokenValue, deviceInfoStr, expiryDate);
        
        // 4. Lưu hoặc cập nhật FCM token nếu có
        log.info("🔔 [FCM VERIFY OTP] Kiểm tra FCM token: {}", fcmToken != null ? "CÓ" : "NULL");
        if (fcmToken != null && !fcmToken.trim().isEmpty()) {
            log.info("🔔 [FCM VERIFY OTP] Bắt đầu lưu FCM token cho user: {}", user.getId());
            
            com.greenconnect.greenconnect_api.enums.DeviceType deviceTypeEnum = parseDeviceType(deviceType);
            log.info("🔔 [FCM VERIFY OTP] Device Type parsed: {}", deviceTypeEnum);
            saveOrUpdateFcmToken(user, fcmToken, deviceTypeEnum);
            log.info("🔔 [FCM VERIFY OTP] Đã lưu FCM token xong");
        } else {
            log.warn("⚠️ [FCM VERIFY OTP] FCM token NULL hoặc EMPTY - không lưu token");
        }
        
        // 5. Load user addresses và favorites
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = loadUserAddresses(user.getId());
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(user.getId());
        
        // 6. Tạo response
        long accessTokenExpiresIn = jwtUtils.getAccessTokenExpiration();
        long refreshTokenExpiresIn = 30 * 24 * 60 * 60; // 30 days
        
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshToken(refreshTokenValue)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .tokenType("Bearer")
                .issuedAt(LocalDateTime.now())
                .user(UserMapper.toUserResponse(user))
                .addresses(addresses)
                .favorites(favorites)
                .build();
        
        log.info("✅ [LOGIN SUCCESS] Đăng nhập thành công cho user: {} với {} địa chỉ và {} favorites", 
                user.getEmail(), addresses.size(), favorites.size());
        return loginResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.GetNonCustomerUsersResponse getNonCustomerUsers(
            com.greenconnect.greenconnect_api.dtos.request.GetNonCustomerUsersRequest request,
            java.util.UUID excludeUserId) {
        
        log.info("📋 [GET NON-CUSTOMER USERS] Admin lấy danh sách users không phải CUSTOMER role, exclude: {}", excludeUserId);
        log.info("📋 [GET NON-CUSTOMER USERS] Page: {}, Size: {}, Sort: {} ({})", 
                request.getPageNumber(), request.getPageSize(), request.getSort(), request.getSortDirection());
        
        // Validate input
        if (request.getPageNumber() == null || request.getPageNumber() < 0) {
            request.setPageNumber(0);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(20);
        }
        if (request.getSort() == null || request.getSort().isEmpty()) {
            request.setSort("createdAt");
        }
        if (request.getSortDirection() == null || request.getSortDirection().isEmpty()) {
            request.setSortDirection("DESC");
        }
        
        // Validate sort field
        String sortField = request.getSort();
        if (!isSortFieldValid(sortField)) {
            log.warn("❌ [GET NON-CUSTOMER USERS] Sort field không hợp lệ: {}", sortField);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // Build Pageable
        org.springframework.data.domain.Sort.Direction direction = 
                request.getSortDirection().equalsIgnoreCase("ASC") ? 
                org.springframework.data.domain.Sort.Direction.ASC : 
                org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(
                        request.getPageNumber(), 
                        request.getPageSize(),
                        org.springframework.data.domain.Sort.by(direction, sortField)
                );
        
        // Query repository
        org.springframework.data.domain.Page<User> userPage = userRepository.findNonCustomerUsers(pageable);
        
        // Filter out excluded user và map to UserResponse
        org.springframework.data.domain.Page<UserResponse> userResponsePage = userPage
                .map(user -> {
                    // Loại bỏ user có ID = excludeUserId
                    if (excludeUserId != null && user.getId().equals(excludeUserId)) {
                        return null;
                    }
                    return UserMapper.toUserResponse(user);
                })
                .map(userResponse -> userResponse); // Keep non-null
        
        // Filter null values (excluded user)
        java.util.List<UserResponse> filteredList = userResponsePage.getContent().stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        
        // Create new Page with filtered content
        org.springframework.data.domain.Page<UserResponse> filteredPage = 
                new org.springframework.data.domain.PageImpl<>(
                        filteredList,
                        pageable,
                        userResponsePage.getTotalElements() - (filteredList.size() < userResponsePage.getContent().size() ? 1 : 0)
                );
        
        log.info("✅ [GET NON-CUSTOMER USERS] Tìm thấy {} users (đã loại bỏ admin hiện tại), trang {}/{}", 
                filteredPage.getTotalElements(), 
                request.getPageNumber() + 1, 
                filteredPage.getTotalPages());
        
        return com.greenconnect.greenconnect_api.dtos.response.GetNonCustomerUsersResponse
                .fromPage(filteredPage, request.getPageNumber());
    }
    
    /**
     * Kiểm tra xem sort field có hợp lệ không.
     */
    private boolean isSortFieldValid(String sortField) {
        return sortField != null && 
               (sortField.equals("id") || 
                sortField.equals("email") || 
                sortField.equals("fullName") || 
                sortField.equals("createdAt") || 
                sortField.equals("updatedAt"));
    }
    
    // ========== FILTER USERS IMPLEMENTATION ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public FilterUsersResponse filterUsers(FilterUsersRequest request, java.util.UUID excludeUserId) {
        log.info("🔍 [FILTER USERS] Admin đang lọc danh sách users, exclude: {}", excludeUserId);
        
        // 1. Validate input
        if (request == null) {
            log.warn("❌ [FILTER] Request null");
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // Validate status (optional - null hoặc rỗng = lấy tất cả status)
        final UserStatus userStatus;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                userStatus = UserStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("❌ [FILTER] Status không hợp lệ: {}", request.getStatus());
                throw new AppException(ErrorCode.VALIDATION_ERROR, 
                        "Status không hợp lệ. Hỗ trợ: ACTIVE, INACTIVE, PENDING_ACTIVATION, BANNED");
            }
        } else {
            userStatus = null; // Lấy tất cả status
        }
        
        // Validate pagination
        if (request.getPageNumber() == null || request.getPageNumber() < 0) {
            request.setPageNumber(0);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(20);
        }
        
        // Validate sortBy
        if (request.getSortBy() == null || request.getSortBy().trim().isEmpty()) {
            request.setSortBy("createdAt");
        }
        String validSortFields = "id,email,fullName,createdAt,updatedAt,status";
        if (!validSortFields.contains(request.getSortBy())) {
            log.warn("❌ [FILTER] Sort field không hợp lệ: {}", request.getSortBy());
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "Sort field không hợp lệ. Hỗ trợ: id, email, fullName, createdAt, updatedAt, status");
        }
        
        // Validate sortDirection
        if (request.getSortDirection() == null || request.getSortDirection().trim().isEmpty()) {
            request.setSortDirection("DESC");
        }
        String sortDirUpper = request.getSortDirection().trim().toUpperCase();
        if (!sortDirUpper.equals("ASC") && !sortDirUpper.equals("DESC")) {
            log.warn("❌ [FILTER] Sort direction không hợp lệ: {}", request.getSortDirection());
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "Sort direction không hợp lệ. Hỗ trợ: ASC, DESC");
        }
        
        // 2. Parse roles trực tiếp từ tiếng Anh (không cần convert)
        final Set<Role> filterRoles = new java.util.HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleStr : request.getRoles()) {
                try {
                    Role role = Role.valueOf(roleStr.trim().toUpperCase());
                    filterRoles.add(role);
                    log.debug("✅ Parsed role: {} → {}", roleStr, role);
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ [FILTER] Role không hợp lệ: {}", roleStr);
                    throw new AppException(ErrorCode.VALIDATION_ERROR, 
                            "Role không hợp lệ: " + roleStr + ". Hỗ trợ: CUSTOMER, ADMIN, ORDER_MANAGER, PRODUCT_MANAGER, MARKETING_MANAGER, CUSTOMER_SUPPORT, SHIPPER");
                }
            }
        }
        
        // 3. Tạo Pageable với sort direction
        final Sort.Direction direction = Sort.Direction.fromString(sortDirUpper);
        final Sort sort = Sort.by(direction, request.getSortBy());
        final Pageable pageable = PageRequest.of(request.getPageNumber(), request.getPageSize(), sort);
        
        log.info("📋 [FILTER] Tiêu chí: status={}, roles={}, startDate={}, endDate={}, exclude={}", 
                request.getStatus(), request.getRoles(), request.getStartDate(), request.getEndDate(), excludeUserId);
        
        // 4. Xây dựng Specification động cho truy vấn
        Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            // 4.0 Loại bỏ user có ID = excludeUserId
            if (excludeUserId != null) {
                predicates.add(cb.notEqual(root.get("id"), excludeUserId));
                log.debug("✅ Added predicate: id != {}", excludeUserId);
            }
            
            // 4.1 Filter theo status (nếu có)
            if (userStatus != null) {
                predicates.add(cb.equal(root.get("status"), userStatus));
                log.debug("✅ Added predicate: status = {}", userStatus);
            }
            
            // 4.1.5 Filter theo searchField (tìm kiếm trong tên, email, điện thoại)
            if (request.getSearchField() != null && !request.getSearchField().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearchField().trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("fullName")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("phoneNumber")), searchPattern)
                );
                predicates.add(searchPredicate);
                log.debug("✅ Added predicate: searchField LIKE {}", searchPattern);
            }
            
            // 4.2 Filter theo roles - QUAN TRỌNG: Loại bỏ CUSTOMER role
            if (!filterRoles.isEmpty()) {
                // Nếu admin chỉ định roles cụ thể → filter theo roles đó
                jakarta.persistence.criteria.Join<User, UserRole> roleJoin = 
                        root.join("userRoles", JoinType.INNER);
                
                predicates.add(cb.and(
                        roleJoin.get("role").in(filterRoles),
                        cb.equal(roleJoin.get("active"), true)
                ));
                log.debug("✅ Added predicate: roles IN {} AND active = true", filterRoles);
            } else {
                // ⚠️ QUAN TRỌNG: Nếu KHÔNG chỉ định roles → loại bỏ CUSTOMER tự động
                // Vì endpoint này là cho NON-CUSTOMER users (staff)
                jakarta.persistence.criteria.Join<User, UserRole> roleJoin = 
                        root.join("userRoles", JoinType.INNER);
                
                predicates.add(cb.and(
                        cb.notEqual(roleJoin.get("role"), Role.CUSTOMER),
                        cb.equal(roleJoin.get("active"), true)
                ));
                log.debug("✅ Added predicate: role != CUSTOMER AND active = true (auto-exclude CUSTOMER)");
            }
            
            // 4.3 Filter theo startDate (nếu có)
            if (request.getStartDate() != null) {
                java.time.LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
                log.debug("✅ Added predicate: createdAt >= {}", startDateTime);
            }
            
            // 4.4 Filter theo endDate (nếu có)
            if (request.getEndDate() != null) {
                java.time.LocalDateTime endDateTime = request.getEndDate().atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
                log.debug("✅ Added predicate: createdAt <= {}", endDateTime);
            }
            
            // Combine tất cả predicates với AND
            if (predicates.isEmpty()) {
                return cb.conjunction(); // Trả về true nếu không có predicate nào
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        
        // 5. Thực hiện truy vấn
        Page<User> userPage = userRepository.findAll(spec, pageable);
        
        log.info("✅ [FILTER] Tìm thấy {} users (trang {}/{})", 
                userPage.getNumberOfElements(), 
                userPage.getNumber() + 1, 
                userPage.getTotalPages());
        
        // 6. Convert sang Response
        List<UserResponse> userResponses = userPage.getContent().stream()
                .map(UserMapper::toUserResponse)
                .collect(Collectors.toList());
        
        // 7. Tạo response
        FilterUsersResponse response = FilterUsersResponse.builder()
                .users(userResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .build();
        
        log.info("✅ [FILTER USERS] Lọc thành công với {} users", userResponses.size());
        return response;
    }

    // ========== CUSTOMER USERS MANAGEMENT ==========

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.GetCustomersResponse getCustomers(
            com.greenconnect.greenconnect_api.dtos.request.GetCustomersRequest request) {
        
        log.info("📋 [GET CUSTOMERS] Lấy danh sách khách hàng (CUSTOMER role)");
        log.info("📋 [GET CUSTOMERS] Page: {}, Size: {}, Sort: {} ({})", 
                request.getPageNumber(), request.getPageSize(), request.getSort(), request.getSortDirection());
        
        // Validate input
        if (request.getPageNumber() == null || request.getPageNumber() < 0) {
            request.setPageNumber(0);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(20);
        }
        if (request.getSort() == null || request.getSort().isEmpty()) {
            request.setSort("createdAt");
        }
        if (request.getSortDirection() == null || request.getSortDirection().isEmpty()) {
            request.setSortDirection("DESC");
        }
        
        // Validate sort field
        String sortField = request.getSort();
        if (!isSortFieldValid(sortField)) {
            log.warn("❌ [GET CUSTOMERS] Sort field không hợp lệ: {}", sortField);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // Build Pageable
        org.springframework.data.domain.Sort.Direction direction = 
                request.getSortDirection().equalsIgnoreCase("ASC") ? 
                org.springframework.data.domain.Sort.Direction.ASC : 
                org.springframework.data.domain.Sort.Direction.DESC;
        
        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(
                        request.getPageNumber(), 
                        request.getPageSize(),
                        org.springframework.data.domain.Sort.by(direction, sortField)
                );
        
        // Query repository - chỉ lấy users có CUSTOMER role
        org.springframework.data.domain.Page<User> userPage = userRepository.findCustomerUsers(pageable);
        
        // Map to UserResponse với địa chỉ mặc định
        org.springframework.data.domain.Page<UserResponse> userResponsePage = userPage
                .map(user -> {
                    // Lấy địa chỉ mặc định của user (isDefault=true)
                    java.util.List<com.greenconnect.greenconnect_api.entities.Address> addresses = 
                            addressRepository.findByUser_Id(user.getId());
                    
                    // Tìm address có isDefault=true
                    com.greenconnect.greenconnect_api.entities.Address defaultAddress = addresses.stream()
                            .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                            .findFirst()
                            .orElse(null);
                    
                    // Map User + Address thành UserResponse
                    return UserMapper.toUserResponseWithAddress(user, defaultAddress);
                });
        
        log.info("✅ [GET CUSTOMERS] Tìm thấy {} customers, trang {}/{}", 
                userResponsePage.getTotalElements(), 
                request.getPageNumber() + 1, 
                userResponsePage.getTotalPages());
        
        return com.greenconnect.greenconnect_api.dtos.response.GetCustomersResponse
                .fromPage(userResponsePage, request.getPageNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.FilterCustomersResponse filterCustomers(
            com.greenconnect.greenconnect_api.dtos.request.FilterCustomersRequest request) {
        
        log.info("🔍 [FILTER CUSTOMERS] Lọc danh sách khách hàng");
        
        // 1. Validate input
        if (request == null) {
            log.warn("❌ [FILTER CUSTOMERS] Request null");
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        // Validate status (optional - null hoặc rỗng = lấy tất cả status)
        final UserStatus userStatus;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                userStatus = UserStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("❌ [FILTER CUSTOMERS] Status không hợp lệ: {}", request.getStatus());
                throw new AppException(ErrorCode.VALIDATION_ERROR, 
                        "Status không hợp lệ. Hỗ trợ: ACTIVE, INACTIVE, PENDING_ACTIVATION, BANNED");
            }
        } else {
            userStatus = null; // Lấy tất cả status
        }
        
        // Validate pagination
        if (request.getPageNumber() == null || request.getPageNumber() < 0) {
            request.setPageNumber(0);
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            request.setPageSize(20);
        }
        
        // Validate sortBy
        if (request.getSortBy() == null || request.getSortBy().trim().isEmpty()) {
            request.setSortBy("createdAt");
        }
        String validSortFields = "id,email,fullName,createdAt,updatedAt,status";
        if (!validSortFields.contains(request.getSortBy())) {
            log.warn("❌ [FILTER CUSTOMERS] Sort field không hợp lệ: {}", request.getSortBy());
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "Sort field không hợp lệ. Hỗ trợ: id, email, fullName, createdAt, updatedAt, status");
        }
        
        // Validate sortDirection
        if (request.getSortDirection() == null || request.getSortDirection().trim().isEmpty()) {
            request.setSortDirection("DESC");
        }
        String sortDirUpper = request.getSortDirection().trim().toUpperCase();
        if (!sortDirUpper.equals("ASC") && !sortDirUpper.equals("DESC")) {
            log.warn("❌ [FILTER CUSTOMERS] Sort direction không hợp lệ: {}", request.getSortDirection());
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "Sort direction không hợp lệ. Hỗ trợ: ASC, DESC");
        }
        
        // 2. Tạo Pageable với sort direction
        final Sort.Direction direction = Sort.Direction.fromString(sortDirUpper);
        final Sort sort = Sort.by(direction, request.getSortBy());
        final Pageable pageable = PageRequest.of(request.getPageNumber(), request.getPageSize(), sort);
        
        log.info("📋 [FILTER CUSTOMERS] Tiêu chí: status={}, startDate={}, endDate={}", 
                request.getStatus(), request.getStartDate(), request.getEndDate());
        
        // 3. Xây dựng Specification động cho truy vấn - CHỈ LẤY CUSTOMER ROLE
        Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            // 3.1 BẮT BUỘC: Chỉ lấy users có CUSTOMER role
            jakarta.persistence.criteria.Join<User, UserRole> roleJoin = 
                    root.join("userRoles", JoinType.INNER);
            
            predicates.add(cb.and(
                    cb.equal(roleJoin.get("role"), Role.CUSTOMER),
                    cb.equal(roleJoin.get("active"), true)
            ));
            log.debug("✅ Added predicate: role = CUSTOMER AND active = true");
            
            // 3.2 Filter theo status (nếu có)
            if (userStatus != null) {
                predicates.add(cb.equal(root.get("status"), userStatus));
                log.debug("✅ Added predicate: status = {}", userStatus);
            }
            
            // 3.3 Filter theo searchField (tìm kiếm trong tên, email, điện thoại)
            if (request.getSearchField() != null && !request.getSearchField().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearchField().trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("fullName")), searchPattern),
                    cb.like(cb.lower(root.get("email")), searchPattern),
                    cb.like(cb.lower(root.get("phoneNumber")), searchPattern)
                );
                predicates.add(searchPredicate);
                log.debug("✅ Added predicate: searchField LIKE {}", searchPattern);
            }
            
            // 3.4 Filter theo startDate (nếu có)
            if (request.getStartDate() != null) {
                java.time.LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
                log.debug("✅ Added predicate: createdAt >= {}", startDateTime);
            }
            
            // 3.5 Filter theo endDate (nếu có)
            if (request.getEndDate() != null) {
                java.time.LocalDateTime endDateTime = request.getEndDate().atTime(23, 59, 59);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
                log.debug("✅ Added predicate: createdAt <= {}", endDateTime);
            }
            
            // Combine tất cả predicates với AND
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        
        // 4. Thực hiện truy vấn
        Page<User> userPage = userRepository.findAll(spec, pageable);
        
        log.info("✅ [FILTER CUSTOMERS] Tìm thấy {} customers (trang {}/{})", 
                userPage.getNumberOfElements(), 
                userPage.getNumber() + 1, 
                userPage.getTotalPages());
        
        // 5. Convert sang Response với địa chỉ mặc định
        List<UserResponse> customerResponses = userPage.getContent().stream()
                .map(user -> {
                    // Lấy địa chỉ mặc định của user (isDefault=true)
                    java.util.List<com.greenconnect.greenconnect_api.entities.Address> addresses = 
                            addressRepository.findByUser_Id(user.getId());
                    
                    // Tìm address có isDefault=true
                    com.greenconnect.greenconnect_api.entities.Address defaultAddress = addresses.stream()
                            .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                            .findFirst()
                            .orElse(null);
                    
                    // Map User + Address thành UserResponse
                    return UserMapper.toUserResponseWithAddress(user, defaultAddress);
                })
                .collect(Collectors.toList());
        
        // 6. Tạo response
        com.greenconnect.greenconnect_api.dtos.response.FilterCustomersResponse response = 
                com.greenconnect.greenconnect_api.dtos.response.FilterCustomersResponse.builder()
                .customers(customerResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .build();
        
        log.info("✅ [FILTER CUSTOMERS] Lọc thành công với {} customers", customerResponses.size());
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public com.greenconnect.greenconnect_api.dtos.response.UpdateUserStatusResponse updateUserStatus(
            String email,
            com.greenconnect.greenconnect_api.dtos.request.UpdateUserStatusRequest request,
            String updatedByEmail) {
        
        log.info("🔄 [UPDATE USER STATUS] Bắt đầu cập nhật trạng thái cho user: {}", email);
        log.info("🔄 [UPDATE USER STATUS] Trạng thái mới: {}, Người thực hiện: {}", request.getStatus(), updatedByEmail);
        
        // 1. Validate status - chỉ cho phép ACTIVE hoặc INACTIVE
        if (request.getStatus() != UserStatus.ACTIVE && request.getStatus() != UserStatus.INACTIVE) {
            log.warn("❌ [UPDATE USER STATUS] Status không hợp lệ: {}", request.getStatus());
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "Chỉ cho phép cập nhật trạng thái ACTIVE hoặc INACTIVE. Sử dụng: ACTIVE, INACTIVE");
        }
        
        // 2. Tìm user theo email
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.error("❌ [UPDATE USER STATUS] User không tồn tại: {}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 3. Lưu trạng thái cũ
        UserStatus previousStatus = user.getStatus();
        
        // 4. Kiểm tra nếu trạng thái mới giống trạng thái cũ
        if (previousStatus == request.getStatus()) {
            log.warn("⚠️ [UPDATE USER STATUS] Trạng thái mới giống trạng thái hiện tại: {}", previousStatus);
            throw new AppException(ErrorCode.VALIDATION_ERROR, 
                    "User đã ở trạng thái " + request.getStatus() + " rồi");
        }
        
        // 5. Cập nhật trạng thái
        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("✅ [UPDATE USER STATUS] Đã cập nhật trạng thái: {} → {}", previousStatus, request.getStatus());
        
        // 6. Gửi thông báo WebSocket nếu tài khoản bị vô hiệu hóa
        if (request.getStatus() == UserStatus.INACTIVE) {
            log.info("🚨 [UPDATE USER STATUS] Tài khoản bị vô hiệu hóa, gửi thông báo force logout");
            try {
                roleChangeNotificationService.notifyAccountDisabled(email);
            } catch (Exception e) {
                log.warn("⚠️ [UPDATE USER STATUS] Không gửi được WebSocket notification: {}", e.getMessage());
                // Không throw exception vì việc cập nhật status đã thành công
            }
        }
        
        // 7. Map sang UserResponse
        UserResponse userResponse = UserMapper.toUserResponse(user);
        
        // 8. Tạo response
        com.greenconnect.greenconnect_api.dtos.response.UpdateUserStatusResponse response = 
                com.greenconnect.greenconnect_api.dtos.response.UpdateUserStatusResponse.builder()
                .user(userResponse)
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .reason(request.getReason())
                .updatedAt(LocalDateTime.now())
                .updatedBy(updatedByEmail)
                .build();
        
        log.info("✅ [UPDATE USER STATUS] Hoàn tất cập nhật trạng thái cho user: {}", email);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> getUserFavorites(java.util.UUID userId) {
        log.info("📋 [GET USER FAVORITES] Lấy danh sách yêu thích của user: {}", userId);
        
        // 1. Kiểm tra user có tồn tại không
        if (!userRepository.existsById(userId)) {
            log.warn("❌ [GET USER FAVORITES] Không tìm thấy user với ID: {}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 2. Load danh sách favorites của user (chỉ lấy các favorite đang active)
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = loadUserFavorites(userId);
        
        log.info("✅ [GET USER FAVORITES] Đã lấy {} sản phẩm yêu thích cho user: {}", favorites.size(), userId);
        return favorites;
    }

    // ========== 2FA RESET IMPLEMENTATION ==========

    /**
     * {@inheritDoc}
     * Reset Google Authenticator (2FA) cho user khi mất điện thoại.
     * <p>⚠️ CHỈ ADMIN mới được gọi method này (đã check ở Controller layer)</p>
     * <p>🔄 Logic:</p>
     * <ol>
     *   <li>Set secretKey2FA = NULL</li>
     *   <li>Set totpEnabled = FALSE</li>
     *   <li>User sẽ phải thiết lập lại 2FA ở lần đăng nhập tiếp theo</li>
     * </ol>
     */
    @Override
    @Transactional
    public UserResponse reset2FA(String email) {
        log.info("🔄 [RESET 2FA] Bắt đầu reset Google Authenticator cho user: {}", email);
        
        // 1. Tìm user theo email
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.error("❌ [RESET 2FA] Không tìm thấy user với email: {}", email);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Kiểm tra xem user có đang bật 2FA không
        if (user.getTotpEnabled() == null || !user.getTotpEnabled()) {
            log.warn("⚠️ [RESET 2FA] User '{}' chưa kích hoạt 2FA, không cần reset", email);
            // Vẫn trả về response nhưng log warning
        }
        
        // 3. Reset 2FA fields (dùng đúng tên field: secretKey2FA và totpEnabled)
        user.setSecretKey2FA(null);           // Xóa TOTP secret key
        user.setTotpEnabled(false);           // Tắt 2FA
        
        // 4. Save changes
        User updatedUser = userRepository.save(user);
        
        log.info("✅ [RESET 2FA] Đã reset 2FA thành công cho user: {}", email);
        log.info("✅ [RESET 2FA] secretKey2FA = NULL, totpEnabled = FALSE");
        
        // 5. Return UserResponse
        return UserMapper.toUserResponse(updatedUser);
    }
}
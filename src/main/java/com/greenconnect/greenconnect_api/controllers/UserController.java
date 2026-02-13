package com.greenconnect.greenconnect_api.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.LoginFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.LoginRequest;
import com.greenconnect.greenconnect_api.dtos.request.LogoutRequest;
import com.greenconnect.greenconnect_api.dtos.request.RefreshTokenRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateUserRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.dtos.response.RegisterResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserProfileResponse;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.InvitationService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;
import com.greenconnect.greenconnect_api.services.UserService;
import com.greenconnect.greenconnect_api.utils.JwtUtils;

import java.time.LocalDateTime;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private InvitationService invitationService;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY
    private ElasticsearchSyncService elasticsearchSyncService;

    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY
    private RecombeeSyncService recombeeSyncService;

    @Autowired 
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Đăng ký tài khoản mới với email và password.
     * <p>Provider: LOCAL | Role: CUSTOMER (mặc định)</p>
     * <p>Trả về RegisterResponse chứa access token, refresh token và thông tin user mới.</p>
     * <p>⭐ Khác với login: có thêm trường isNewUser=true, requiresEmailVerification, status, statusCode</p>
     */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /users/register - Đăng ký user với email: {}", request.getEmail());
        
        try {
            LoginResponse loginResponse = userService.registerUser(request);
            
            // Chuyển đổi LoginResponse sang RegisterResponse
            RegisterResponse response = RegisterResponse.builder()
                .status("SUCCESS")
                .statusCode(201) // Created status code
                .message("Đăng ký thành công. Vui lòng đăng nhập với tài khoản của bạn.")
                .build();
            
            log.info("✅ Đăng ký thành công cho user: {}", request.getEmail());
            return ResponseUtil.success(response, "Đăng ký thành công");
            
        } catch (Exception e) {
            log.error("❌ Đăng ký thất bại cho email: {} - Lỗi: {}", request.getEmail(), e.getMessage());
            
            // Trả về RegisterResponse với status ERROR (không có tokens)
            RegisterResponse errorResponse = RegisterResponse.builder()
                .status("ERROR")
                .statusCode(400) // Bad Request
                .message(e.getMessage())
                .build();
            
            return ApiResponse.<RegisterResponse>builder()
                .code(400)
                .message("Đăng ký thất bại: " + e.getMessage())
                .data(errorResponse)
                .build();
        }
    }
    
    /**
     * Đăng ký hoặc đăng nhập bằng Firebase (Google).
     * <p>Provider: GOOGLE | Role: CUSTOMER (mặc định nếu user mới)</p>
     * <p>Nếu user đã tồn tại → đăng nhập | Nếu user mới → tạo mới + đăng nhập</p>
     * <p>Trả về RegisterResponse với status=SUCCESS, statusCode=201 (new user) hoặc 200 (existing user)</p>
     */
   @PostMapping("/google-register")
    public ApiResponse<RegisterResponse> firebaseRegister(@Valid @RequestBody RegisterFirebaseRequest request) {
        log.info("POST /users/firebase-register - Đăng ký Firebase với email: {} và provider: GOOGLE", request.getEmail());
        
        try {
            LoginResponse loginResponse = userService.registerFirebaseUser(request);
            
            // Chuyển đổi LoginResponse sang RegisterResponse
            RegisterResponse response = RegisterResponse.builder()
                .status("SUCCESS")
                .statusCode(201) // Created status code cho Firebase register
                .message("Đăng ký Firebase thành công. Bạn có thể bắt đầu sử dụng ứng dụng.")
                .build();
            
            log.info("✅ Đăng ký Firebase thành công cho user: {}", request.getEmail());
            return ResponseUtil.success(response, "Đăng ký Firebase thành công");
            
        } catch (Exception e) {
            log.error("❌ Đăng ký Firebase thất bại cho email: {} - Lỗi: {}", request.getEmail(), e.getMessage());
            
            // Trả về RegisterResponse với status ERROR
            RegisterResponse errorResponse = RegisterResponse.builder()
                .status("ERROR")
                .statusCode(400) // Bad Request
                .message(e.getMessage())
                .build();
            
            return ApiResponse.<RegisterResponse>builder()
                .code(400)
                .message("Đăng ký tài khoản thất bại: " + e.getMessage())
                .data(errorResponse)
                .build();
        }
    }
    
    /**
     * Đăng nhập user với email và password.
     * <p>Trả về access token và refresh token để frontend lưu trữ.</p>
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // elasticsearchSyncService.syncAllProducts();

        log.info("🔐 [CUSTOMER LOGIN] Đăng nhập với email: {}", request.getEmail());
        log.info("🔔 [CUSTOMER LOGIN] FCM Token: {}", 
            request.getFcmToken() != null ? 
            request.getFcmToken().substring(0, Math.min(30, request.getFcmToken().length())) + "..." : 
            "NULL");
        log.info("🔔 [CUSTOMER LOGIN] Device Type: {}", request.getDeviceType() != null ? request.getDeviceType() : "NULL");
        
        LoginResponse response = userService.loginUser(request);
        return ResponseUtil.success(response, "Đăng nhập thành công");
    }
    
    /**
     * Refresh access token bằng refresh token.
     * <p>Frontend gọi khi access token hết hạn để lấy token mới mà không cần đăng nhập lại.</p>
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /users/refresh - Làm mới token");
        LoginResponse response = userService.refreshToken(request);
        return ResponseUtil.success(response, "Làm mới token thành công");
    }
    
    /**
     * Đăng nhập user bằng Firebase (Google).
     * <p>Kiểm tra user tồn tại bằng email và providerId, nếu có thì trả về token.</p>
     * <p>Lưu device info + FCM token để track multi-device và push notifications.</p>
     */
    @PostMapping("/google-login")
    public ApiResponse<LoginResponse> firebaseLogin(@Valid @RequestBody LoginFirebaseRequest request) {
        log.info("🔐 [CUSTOMER GOOGLE LOGIN] Đăng nhập Firebase với email: {}", request.getEmail());
        log.info("🔔 [CUSTOMER GOOGLE LOGIN] FCM Token: {}", 
            request.getFcmToken() != null ? 
            request.getFcmToken().substring(0, Math.min(30, request.getFcmToken().length())) + "..." : 
            "NULL");
        log.info("🔔 [CUSTOMER GOOGLE LOGIN] Device Type: {}", request.getDeviceType() != null ? request.getDeviceType() : "NULL");
        
        LoginResponse response = userService.loginFirebaseUser(request);
        return ResponseUtil.success(response, "Đăng nhập Firebase thành công");
    }
    
    /**
     * Cập nhật thông tin người dùng.
     * <p>User chỉ có thể update thông tin cơ bản (fullName, phoneNumber, avatarUrl, password).
     * Admin có thể update tất cả field bao gồm role, status, loyaltyPoints.</p>
     */
    @PutMapping("/profile/{userId}")
    //@PreAuthorize("#userId == authentication.principal.id")
    public ApiResponse<UserResponse> updateProfile(@PathVariable UUID userId,@Valid @RequestBody UpdateUserRequest request) {
        // Lấy token từ Authorization header (format: "Bearer {token}")
        
        // Lấy user ID từ JWT token
        
        // Lấy TẤT CẢ ROLES từ scope thay vì chỉ primary role
        
        UserResponse response = userService.updateUser(userId, request);
        return ResponseUtil.success(response, "Cập nhật thông tin thành công");
    }

    /**
     * Lấy thông tin user theo ID (dành cho Admin và Customer Support).
     * <p>✅ Trả về UserResponse với đầy đủ thông tin bao gồm defaultAddress</p>
     * <p>⚠️ Endpoint này dành cho Admin/Customer Support để xem thông tin khách hàng</p>
     * <p>Response bao gồm:</p>
     * <ul>
     *   <li>Thông tin cơ bản: id, email, fullName, phoneNumber, avatarUrl</li>
     *   <li>Thông tin tài khoản: status, roles, provider, totpEnabled</li>
     *   <li>Thông tin business: loyaltyPoints, totalPaymentAmount</li>
     *   <li>Thông tin địa chỉ: defaultAddress (địa chỉ mặc định của user)</li>
     *   <li>Timestamp: createdAt, updatedAt</li>
     * </ul>
     * 
     * @param userId UUID của user cần lấy thông tin
     * @return UserResponse với defaultAddress
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID userId) {
        log.info("GET /users/{} - Lấy thông tin user theo ID", userId);
        
        UserResponse response = userService.getUserById(userId);
        
        log.info("✅ Lấy thông tin user thành công: {} (email: {})", userId, response.getEmail());
        return ResponseUtil.success(response, "Lấy thông tin user thành công");
    }

    /**
     * Lấy profile user theo email.
     * <p>Endpoint này cho phép lấy thông tin đầy đủ của user thông qua email</p>
     * <p>✅ Trả về: id, email, fullName, phoneNumber, avatarUrl, status, roles, provider, createdAt, updatedAt</p>
     * <p>⚠️ Public endpoint - Có thể sử dụng cho nhiều mục đích:</p>
     * <ul>
     *   <li>Admin/Support xem thông tin user</li>
     *   <li>User xem profile của chính mình</li>
     *   <li>Chat system lấy thông tin người nhận tin nhắn</li>
     *   <li>Order system lấy thông tin khách hàng</li>
     * </ul>
     * 
     * @param email Email của user cần lấy profile (path variable)
     * @return UserResponse chứa thông tin đầy đủ của user
     */
    @GetMapping("/profile/email/{email:.+}")
    public ApiResponse<UserResponse> getUserProfileByEmail(@PathVariable String email) {
        log.info("GET /users/profile/email/{} - Lấy profile user theo email", email);
        
        UserResponse response = userService.getUserByEmail(email);
        
        log.info("✅ Lấy profile thành công cho user: {} (ID: {})", email, response.getId());
        return ResponseUtil.success(response, "Lấy thông tin user thành công");
    }

    /**
     * Lấy thông tin đầy đủ của user (user + addresses + favorites).
     * <p>Giống như response sau khi login nhưng KHÔNG có tokens (accessToken/refreshToken).</p>
     * <p>✅ Trả về UserProfileResponse bao gồm:</p>
     * <ul>
     *   <li>userId - User ID để client track</li>
     *   <li>user - Thông tin user đầy đủ (UserResponse)</li>
     *   <li>addresses - Danh sách địa chỉ của user</li>
     *   <li>favorites - Danh sách sản phẩm yêu thích</li>
     *   <li>retrievedAt - Thời gian lấy thông tin</li>
     * </ul>
     * <p>⚠️ Endpoint này yêu cầu authentication - User chỉ có thể xem profile của chính mình</p>
     * 
     * @param userId UUID của user cần lấy profile (path variable)
     * @return UserProfileResponse chứa thông tin đầy đủ của user
     */
    @GetMapping("/profile/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable UUID userId) {
        log.info("GET /users/profile/{} - Lấy thông tin đầy đủ của user", userId);
        
        UserProfileResponse response = userService.getUserProfile(userId);
        
        log.info("✅ Lấy profile đầy đủ thành công cho user: {}", userId);
        return ResponseUtil.success(response, "Lấy thông tin user thành công");
    }



    //     @PutMapping("/profile")
    // @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    // public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
    //         @Valid @RequestBody UserUpdateRequest request) {
    //     UUID currentUserId = SecurityUtils.getCurrentUserId();
    //     UserResponse user = userService.updateUser(currentUserId, request);
    //     return ResponseUtil.success(user, "Cập nhật profile thành công");
    // }

    // ========== LOGOUT & SESSION MANAGEMENT ==========

    /**
     * Đăng xuất từ thiết bị hiện tại.
     * <p>Xóa refresh token cụ thể khỏi database để vô hiệu hóa phiên đăng nhập.</p>
     * <p>Xóa FCM token để ngừng nhận push notifications trên thiết bị này.</p>
     */
        @PostMapping("/logout")
        public ApiResponse<String> logout(@Valid @RequestBody LogoutRequest request,
                                        @RequestHeader("Authorization") String authHeader) {
            // Lấy token từ Authorization header
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            java.util.UUID userId = jwtUtils.getUserIdFromToken(token);
            
            log.info("POST /users/logout - User {} đăng xuất", userId);
            log.info("POST /users/logout - FCM Token: {}", 
                    request.getFcmToken() != null ? request.getFcmToken().substring(0, Math.min(20, request.getFcmToken().length())) + "..." : "NULL");
            
            boolean success = userService.logoutCurrentDevice(userId, request.getRefreshToken(), request.getFcmToken());
            
            if (success) {
                log.info("✅ POST /users/logout - Logout thành công cho user: {}", userId);
                return ResponseUtil.success("Đăng xuất thành công", "Đã xóa phiên đăng nhập và FCM token khỏi thiết bị này");
            } else {
                log.warn("⚠️ POST /users/logout - Logout thất bại cho user: {}", userId);
                return ResponseUtil.success("Logout thất bại", "Refresh token không hợp lệ hoặc đã hết hạn");
            }
        }

    /**
     * Đăng xuất từ tất cả thiết bị.
     * <p>Xóa tất cả refresh tokens để force logout khỏi mọi thiết bị.</p>
     */
    @PostMapping("/logout-all")
    public ApiResponse<String> logoutAllDevices(@RequestHeader("Authorization") String authHeader) {
        // Lấy token từ Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        java.util.UUID userId = jwtUtils.getUserIdFromToken(token);
        
        int loggedOutDevices = userService.logoutAllDevices(userId);
        
        log.info("POST /users/logout-all - Logout {} devices cho user: {}", loggedOutDevices, userId);
        return ResponseUtil.success(
            String.format("Đã đăng xuất từ %d thiết bị", loggedOutDevices),
            "Logout thành công từ tất cả thiết bị"
        );
    }

    /**
     * Lấy danh sách thiết bị đang đăng nhập.
     * <p>Hiển thị thông tin các thiết bị có phiên đăng nhập active.</p>
     */
    @GetMapping("/active-devices")
    public ApiResponse<List<Object>> getActiveDevices(@RequestHeader("Authorization") String authHeader) {
        // Lấy token từ Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        java.util.UUID userId = jwtUtils.getUserIdFromToken(token);
        
        List<Object> activeDevices = userService.getActiveDevices(userId);
        
        log.info("GET /users/active-devices - Lấy {} active devices cho user: {}", activeDevices.size(), userId);
        return ResponseUtil.success(activeDevices, "Lấy danh sách thiết bị thành công");
    }

    // ========== ADDRESS MANAGEMENT ==========

    @Autowired
    private com.greenconnect.greenconnect_api.services.AddressService addressService;
    
    @Autowired
    private com.greenconnect.greenconnect_api.services.FavoriteService favoriteService;
    
    @Autowired
    private com.greenconnect.greenconnect_api.services.ProductService productService;

    /**
     * Lấy danh sách địa chỉ của user.
     * <p>User chỉ có thể xem địa chỉ của chính mình, admin có thể xem tất cả.</p>
     */
    @GetMapping("/{userId}/addresses")
    public ApiResponse<List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone>> getUserAddresses(
            @PathVariable UUID userId) {
        log.info("GET /users/{}/addresses - Lấy danh sách địa chỉ của user", userId);
        
        List<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> addresses = 
            addressService.getUserAddresses(userId);
        
        log.info("✅ Lấy {} địa chỉ cho user: {}", addresses.size(), userId);
        return ResponseUtil.success(addresses, "Lấy danh sách địa chỉ thành công");
    }

    /**
     * Thêm địa chỉ mới cho user.
     * <p>User có thể tự thêm địa chỉ cho mình, admin có thể thêm cho bất kỳ user nào.</p>
     */
    @PostMapping("/addresses")
    public ApiResponse<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> createAddress(
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.AddressRequest request) {
        log.info("POST /users/addresses - Tạo địa chỉ mới");
        log.info("📍 Request data: recipientName={}, phone={}, street={}, type={}, isDefault={}", 
                request.getRecipientName(), request.getRecipientPhone(), 
                request.getStreetAddress(), request.getAddressType(), request.getIsDefault());
        log.info("📍 Province codes - 34: {}, 63: {}", request.getProvinceCode34(), request.getProvinceCode63());
        log.info("📍 District code 63: {}, Ward codes - 34: {}, 63: {}", 
                request.getDistrictCode63(), request.getWardCode34(), request.getWardCode63());
        
        try {
            com.greenconnect.greenconnect_api.dtos.response.AddressRespone response = 
                addressService.createAddress(request);
            log.info("✅ Tạo địa chỉ thành công: {}", response.getId());
            return ResponseUtil.success(response, "Tạo địa chỉ thành công");
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo địa chỉ: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Cập nhật thông tin địa chỉ.
     * <p>User chỉ có thể update địa chỉ của chính mình, admin có thể update tất cả.</p>
     */
    @PutMapping("/addresses/{addressId}")
    public ApiResponse<com.greenconnect.greenconnect_api.dtos.response.AddressRespone> updateAddress(
            @PathVariable UUID addressId,
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.AddressRequest request) {
        log.info("PUT /users/addresses/{} - Cập nhật địa chỉ", addressId);
        
        com.greenconnect.greenconnect_api.dtos.response.AddressRespone response = 
            addressService.updateAddress(addressId, request);
        
        log.info("✅ Cập nhật địa chỉ thành công: {}", addressId);
        return ResponseUtil.success(response, "Cập nhật địa chỉ thành công");
    }

    /**
     * Xóa địa chỉ.
     * <p>User chỉ có thể xóa địa chỉ của chính mình, admin có thể xóa tất cả.</p>
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/addresses/{addressId}")
    public ApiResponse<String> deleteAddress(@PathVariable UUID addressId) {
        log.info("DELETE /users/addresses/{} - Xóa địa chỉ", addressId);
        
        addressService.deleteAddress(addressId);
        
        log.info("✅ Xóa địa chỉ thành công: {}", addressId);
        return ResponseUtil.success("Đã xóa địa chỉ", "Xóa địa chỉ thành công");
    }

    // ========== FAVORITE MANAGEMENT ==========

    /**
     * Thêm sản phẩm vào danh sách yêu thích của user.
     * <p>Giới hạn tối đa 20 sản phẩm yêu thích cho mỗi user.</p>
     * <p>⚠️ Yêu cầu đăng nhập - userId trong path phải khớp với userId từ JWT token.</p>
     * <p>Admin có thể thêm favorite cho bất kỳ user nào.</p>
     */
    @PostMapping("/{userId}/favorites/{productId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<String> addFavorite(
            @PathVariable UUID userId,
            @PathVariable UUID productId,
            @RequestHeader("Authorization") String authHeader) {
        log.info("POST /users/{}/favorites/{} - Thêm sản phẩm vào yêu thích", userId, productId);
        
        favoriteService.addFavorite(userId, productId);
        
        // 🔄 Track bookmark in Recombee
        if (recombeeSyncService != null) {
            try {
                recombeeSyncService.trackBookmark(userId, productId);
                log.info("✅ [RECOMBEE TRACKING] Bookmark tracked → user={}, product={}", userId, productId);
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track bookmark: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ [RECOMBEE] Service not available - Bookmark tracking skipped");
        }
        
        log.info("✅ Thêm sản phẩm {} vào yêu thích của user {} thành công", productId, userId);
        return ResponseUtil.success("Đã thêm vào yêu thích", "Thêm sản phẩm vào danh sách yêu thích thành công");
    }

    /**
     * Xóa sản phẩm khỏi danh sách yêu thích của user.
     * <p>⚠️ Yêu cầu đăng nhập - userId trong path phải khớp với userId từ JWT token.</p>
     * <p>Admin có thể xóa favorite cho bất kỳ user nào.</p>
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/{userId}/favorites/{productId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<String> removeFavorite(
            @PathVariable UUID userId,
            @PathVariable UUID productId,
            @RequestHeader("Authorization") String authHeader) {
        log.info("DELETE /users/{}/favorites/{} - Xóa sản phẩm khỏi yêu thích", userId, productId);
        
        favoriteService.removeFavorite(userId, productId);
        
        log.info("✅ Xóa sản phẩm {} khỏi yêu thích của user {} thành công", productId, userId);
        return ResponseUtil.success("Đã xóa khỏi yêu thích", "Xóa sản phẩm khỏi danh sách yêu thích thành công");
    }

    /**
     * Lấy danh sách sản phẩm yêu thích của user với phân trang.
     * <p>Trả về Page<ProductResponse> với trường isFavorited = true cho tất cả sản phẩm.</p>
     * <p>⚠️ Yêu cầu đăng nhập.</p>
     */
    @GetMapping("/{userId}/favorites")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.ProductResponse>> getFavoriteProducts(
            @PathVariable UUID userId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("GET /users/{}/favorites - Lấy danh sách sản phẩm yêu thích (page: {}, size: {})", userId, page, size);
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> favorites = 
            favoriteService.listFavorites(userId, pageable);
        
        log.info("✅ Lấy {} sản phẩm yêu thích của user {} thành công", favorites.getNumberOfElements(), userId);
        return ResponseUtil.success(favorites, "Lấy danh sách sản phẩm yêu thích thành công");
    }

    /**
     * Lấy danh sách ID sản phẩm yêu thích của user (simple version).
     * <p>Trả về List<FavoriteResponse> chỉ chứa id và productId.</p>
     * <p>⚠️ Endpoint này trả về format đơn giản hơn, phù hợp cho việc sync favorites list.</p>
     * <p>✅ Response format:</p>
     * <pre>
     * "favorites": [
     *   { "id": "uuid", "productId": "uuid" },
     *   { "id": "uuid", "productId": "uuid" }
     * ]
     * </pre>
     * 
     * @param userId UUID của user cần lấy danh sách yêu thích
     * @return List<FavoriteResponse> chứa danh sách favorites với id và productId
     */
    @GetMapping("/{userId}/favorites/list")
    public ApiResponse<List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse>> getUserFavoritesList(
            @PathVariable UUID userId) {
        log.info("GET /users/{}/favorites/list - Lấy danh sách ID yêu thích của user", userId);
        
        List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> favorites = 
            userService.getUserFavorites(userId);
        
        log.info("✅ Lấy {} sản phẩm yêu thích của user {} thành công", favorites.size(), userId);
        return ResponseUtil.success(favorites, "Lấy danh sách sản phẩm yêu thích thành công");
    }

    /**
     * Lấy điểm tích lũy và tổng tiền đã mua của user.
     * <p>Trả về thông tin loyalty points và total payment amount.</p>
     * <p>⚠️ Yêu cầu đăng nhập - userId trong path phải khớp với userId từ JWT token hoặc user là ADMIN.</p>
     * 
     * <p><b>Thông tin trả về:</b></p>
     * <ul>
     *   <li>loyaltyPoints: Điểm tích lũy hiện tại (BigDecimal)</li>
     *   <li>totalPaymentAmount: Tổng tiền đã thanh toán tất cả đơn hàng (BigDecimal)</li>
     * </ul>
     * 
     * <p><b>Ví dụ response:</b></p>
     * <pre>
     * {
     *   "loyaltyPoints": 15000.00,
     *   "totalPaymentAmount": 2500000.00
     * }
     * </pre>
     * 
     * @param userId UUID của user cần lấy thông tin
     * @return Map chứa loyaltyPoints và totalPaymentAmount
     */
    @GetMapping("/{userId}/loyalty-info")
    public ApiResponse<Map<String, java.math.BigDecimal>> getUserLoyaltyInfo(
            @PathVariable UUID userId) {
        log.info("GET /users/{}/loyalty-info - Lấy điểm tích lũy và tổng tiền mua của user", userId);
        
        com.greenconnect.greenconnect_api.entities.User user = userRepository.findById(userId)
            .orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
                com.greenconnect.greenconnect_api.exceptions.ErrorCode.USER_NOT_FOUND));
        
        Map<String, java.math.BigDecimal> loyaltyInfo = new HashMap<>();
        loyaltyInfo.put("loyaltyPoints", user.getLoyaltyPoints());
        loyaltyInfo.put("totalPaymentAmount", user.getTotalPaymentAmount());
         
        log.info("✅ Lấy loyalty info thành công - User: {}, Points: {}, Total: {}", 
            userId, user.getLoyaltyPoints(), user.getTotalPaymentAmount());
        
        return ResponseUtil.success(loyaltyInfo, "Lấy thông tin điểm tích lũy thành công");
    }

}
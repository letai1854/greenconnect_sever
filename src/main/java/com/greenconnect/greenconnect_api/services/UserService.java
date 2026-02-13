package com.greenconnect.greenconnect_api.services;

import java.util.List;

import com.greenconnect.greenconnect_api.dtos.request.LoginFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.LoginRequest;
import com.greenconnect.greenconnect_api.dtos.request.RefreshTokenRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateUserRequest;
import com.greenconnect.greenconnect_api.dtos.request.VerifyOtpRequest;
import com.greenconnect.greenconnect_api.dtos.response.LoginInitialResponse;
import com.greenconnect.greenconnect_api.dtos.response.LoginResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;

/**
 * Service interface for User management operations.
 * <p>Định nghĩa các business operations cho User entity.</p>
 */
public interface UserService {
    
    /**
     * Đăng ký tài khoản mới cho người dùng.
     * <p>Provider: LOCAL | Role: CUSTOMER (mặc định)</p>
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate RegisterRequest</li>
     *   <li>Kiểm tra email đã tồn tại chưa</li>
     *   <li>Hash password</li>
     *   <li>Tạo User entity với provider=LOCAL và role=CUSTOMER</li>
     *   <li>Lưu vào database</li>
     *   <li>Tạo JWT tokens (access token + refresh token)</li>
     *   <li>Trả về LoginResponse với tokens</li>
     * </ul>
     *
     * @param request RegisterRequest (email, password) từ client
     * @return LoginResponse chứa access token, refresh token và user info
     * @throws BusinessException nếu email đã tồn tại hoặc data không hợp lệ
     */
    LoginResponse registerUser(RegisterRequest request);
    
    /**
     * Đăng ký hoặc đăng nhập bằng Firebase (Google).
     * <p>Provider: GOOGLE | Role: CUSTOMER (mặc định nếu user mới)</p>
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate RegisterFirebaseRequest</li>
     *   <li>Kiểm tra user có tồn tại theo email+provider không</li>
     *   <li>Nếu có → đăng nhập (tạo tokens mới)</li>
     *   <li>Nếu không → tạo user mới với provider=GOOGLE, role=CUSTOMER, rồi đăng nhập</li>
     *   <li>Tạo JWT tokens (access token + refresh token)</li>
     *   <li>Trả về LoginResponse với tokens</li>
     * </ul>
     *
     * @param request RegisterFirebaseRequest (idToken, email, fullName, photoURL, providerId) từ Firebase
     * @return LoginResponse chứa access token, refresh token và user info
     * @throws BusinessException nếu Firebase data không hợp lệ
     */
    LoginResponse registerFirebaseUser(RegisterFirebaseRequest request);
    
    /**
     * Đăng nhập bằng email và password.
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate LoginRequest</li>
     *   <li>Tìm user theo email trong UserRepository</li>
     *   <li>Verify password với BCrypt encoder</li>
     *   <li>Tạo JWT access token (15 phút) chứa User data</li>
     *   <li>Tạo refresh token (7 ngày) và lưu vào RefreshToken table</li>
     *   <li>Trả về LoginResponse với tokens và user info</li>
     * </ul>
     *
     * @param request LoginRequest với email và password
     * @return LoginResponse chứa access token, refresh token và user info
     * @throws AppException nếu email không tồn tại hoặc password sai
     */
    LoginResponse loginUser(LoginRequest request);
    
    /**
     * Làm mới access token bằng refresh token.
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate RefreshTokenRequest</li>
     *   <li>Verify refresh token trong database và chưa expire</li>
     *   <li>Extract user info từ refresh token</li>
     *   <li>Tạo access token mới</li>
     *   <li>Tạo refresh token mới và update trong database</li>
     *   <li>Trả về LoginResponse với tokens mới</li>
     * </ul>
     *
     * @param request RefreshTokenRequest với refresh token
     * @return LoginResponse chứa access token mới và refresh token mới
     * @throws AppException nếu refresh token không hợp lệ hoặc đã hết hạn
     */
    LoginResponse refreshToken(RefreshTokenRequest request);
    
    /**
     * Đăng nhập bằng Firebase (Google).
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate LoginFirebaseRequest</li>
     *   <li>Tìm user theo email và provider GOOGLE</li>
     *   <li>Verify providerId khớp với stored hash</li>
     *   <li>Tạo JWT access token và refresh token</li>
     *   <li>Lưu refresh token + device info vào DB</li>
     *   <li>Trả về LoginResponse với tokens và user info</li>
     * </ul>
     *
     * @param request LoginFirebaseRequest chứa Firebase data (email, providerId, device info)
     * @return LoginResponse chứa access token, refresh token và user info
     * @throws AppException nếu user không tồn tại hoặc Firebase data không hợp lệ
     */
    LoginResponse loginFirebaseUser(LoginFirebaseRequest request);
    
    /**
     * Cập nhật thông tin người dùng.
     * <p>Thực hiện các bước:</p>
     * <ul>
     *   <li>Validate UpdateUserRequest</li>
     *   <li>Tìm user theo userId từ JWT token</li>
     *   <li>Chỉ update các field khác null</li>
     *   <li>Hash password nếu có thay đổi</li>
     *   <li>Kiểm tra quyền admin cho role/status/loyaltyPoints</li>
     *   <li>Trả về UserResponse đã cập nhật</li>
     * </ul>
     *
     * @param userId ID của user cần update (lấy từ JWT token)
     * @param request UpdateUserRequest chứa thông tin cần cập nhật
     * @param currentUserRoles Set<Role> của user hiện tại (từ JWT scope - để kiểm tra quyền admin)
     * @return UserResponse chứa thông tin user đã cập nhật
     * @throws AppException nếu user không tồn tại hoặc không có quyền
     */
    UserResponse updateUser(java.util.UUID userId, UpdateUserRequest request);

    /**
     * Lấy danh sách tất cả users trong hệ thống (chỉ admin).
     * <p>Trả về thông tin cơ bản của tất cả users để admin quản lý.</p>
     *
     * @return List<UserResponse> chứa thông tin tất cả users
     */
    List<UserResponse> getAllUsers();

    /**
     * Admin cập nhật thông tin user theo email.
     * <p>Tương tự updateUser nhưng tìm user theo email thay vì UUID.</p>
     *
     * @param email Email của user cần update
     * @param request UpdateUserRequest chứa thông tin cần cập nhật
     * @param currentUserRoles Set<Role> của admin đang thực hiện update
     * @return UserResponse sau khi update
     * @throws BusinessException nếu user không tồn tại hoặc không có quyền
     */
    UserResponse updateUserByEmail(String email, UpdateUserRequest request);

    /**
     * Lấy thông tin user theo ID (từ database, bao gồm defaultAddress).
     * <p>Endpoint dành cho Admin/Customer Support để xem chi tiết khách hàng.</p>
     * <p>Trả về UserResponse với đầy đủ thông tin bao gồm địa chỉ mặc định.</p>
     *
     * @param userId UUID của user cần lấy thông tin
     * @return UserResponse chứa thông tin user + defaultAddress
     * @throws BusinessException nếu user không tồn tại
     */
    UserResponse getUserById(java.util.UUID userId);

    /**
     * Lấy thông tin user theo email (từ database, không cache).
     * <p>Dùng để client refresh profile sau khi nhận WebSocket notification về role change.</p>
     *
     * @param email Email của user cần lấy thông tin
     * @return UserResponse chứa thông tin user mới nhất từ database
     * @throws BusinessException nếu user không tồn tại
     */
    UserResponse getUserByEmail(String email);

    /**
     * Lấy thông tin đầy đủ của user (user info + addresses + favorites).
     * <p>Giống như LoginResponse nhưng KHÔNG bao gồm tokens.</p>
     * <p>Trả về UserProfileResponse chứa:</p>
     * <ul>
     *   <li>userId - User ID để client track</li>
     *   <li>user - Thông tin user đầy đủ (UserResponse)</li>
     *   <li>addresses - Danh sách địa chỉ của user</li>
     *   <li>favorites - Danh sách sản phẩm yêu thích</li>
     *   <li>retrievedAt - Timestamp lấy dữ liệu</li>
     * </ul>
     *
     * @param userId UUID của user cần lấy profile
     * @return UserProfileResponse chứa thông tin đầy đủ của user
     * @throws BusinessException nếu user không tồn tại
     */
    com.greenconnect.greenconnect_api.dtos.response.UserProfileResponse getUserProfile(java.util.UUID userId);


    // ========== LOGOUT & TOKEN MANAGEMENT ==========

    /**
     * Đăng xuất từ thiết bị hiện tại.
     * <p>Xóa refresh token cụ thể khỏi database để vô hiệu hóa phiên đăng nhập.</p>
     * <p>Xóa FCM token của thiết bị này để ngừng nhận push notifications.</p>
     *
     * @param userId ID của user đang logout
     * @param refreshToken Refresh token của thiết bị cần logout
     * @param fcmToken FCM token của thiết bị cần xóa (optional)
     * @return true nếu logout thành công
     */
    boolean logoutCurrentDevice(java.util.UUID userId, String refreshToken, String fcmToken);

    /**
     * Đăng xuất từ tất cả thiết bị.
     * <p>Xóa tất cả refresh tokens của user khỏi database.</p>
     *
     * @param userId ID của user đang logout
     * @return số lượng thiết bị đã logout
     */
    int logoutAllDevices(java.util.UUID userId);

    /**
     * Lấy danh sách thiết bị đang đăng nhập.
     * <p>Trả về thông tin các thiết bị có refresh token còn hạn.</p>
     *
     * @param userId ID của user
     * @return List thông tin các thiết bị active
     */
    List<Object> getActiveDevices(java.util.UUID userId);

    // ========== 2FA (TOTP) AUTHENTICATION ==========

    /**
     * Đăng nhập với kiểm tra 2FA (TOTP) - Email/Password.
     * <p>Nếu user chưa kích hoạt 2FA → trả về QR code</p>
     * <p>Nếu user đã kích hoạt 2FA → yêu cầu nhập OTP</p>
     *
     * @param request LoginRequest với email và password
     * @return LoginInitialResponse chứa thông tin về 2FA status
     * @throws BusinessException nếu email hoặc password không đúng
     */
    LoginInitialResponse loginUserWithTotp(LoginRequest request);

    /**
     * Đăng nhập Firebase với kiểm tra 2FA (TOTP).
     * <p>Nếu user chưa kích hoạt 2FA → trả về QR code</p>
     * <p>Nếu user đã kích hoạt 2FA → yêu cầu nhập OTP</p>
     *
     * @param request LoginFirebaseRequest với Firebase data
     * @return LoginInitialResponse chứa thông tin về 2FA status
     * @throws BusinessException nếu Firebase data không hợp lệ
     */
    LoginInitialResponse loginFirebaseUserWithTotp(LoginFirebaseRequest request);

    /**
     * Xác thực OTP và hoàn tất đăng nhập.
     * <p>Nếu OTP đúng → trả về access token + refresh token</p>
     * <p>Nếu OTP sai → trả về mã lỗi 4890</p>
     *
     * @param request VerifyOtpRequest chứa userId và OTP code
     * @return LoginResponse với tokens nếu thành công
     * @throws BusinessException nếu OTP không hợp lệ
     */
    LoginResponse verifyOtpAndLogin(VerifyOtpRequest request);

    /**
     * Lấy danh sách users không phải CUSTOMER role (chỉ dành cho ADMIN).
     * <p>Trả về tất cả staff users: ORDER_MANAGER, PRODUCT_MANAGER, MARKETING_MANAGER, etc.</p>
     * <p>Hỗ trợ phân trang, sắp xếp với các trường: id, email, fullName, createdAt, updatedAt</p>
     *
     * @param request GetNonCustomerUsersRequest chứa pageNumber, pageSize, sort, sortDirection
     * @param excludeUserId UUID của admin đang thực hiện truy vấn (để loại bỏ khỏi kết quả)
     * @return GetNonCustomerUsersResponse chứa danh sách users và thông tin phân trang
     * @throws BusinessException nếu sort field không hợp lệ
     */
    com.greenconnect.greenconnect_api.dtos.response.GetNonCustomerUsersResponse getNonCustomerUsers(
            com.greenconnect.greenconnect_api.dtos.request.GetNonCustomerUsersRequest request,
            java.util.UUID excludeUserId);

    /**
     * Lọc danh sách users theo nhiều tiêu chí nâng cao.
     * <p>Admin sử dụng endpoint này để lọc users theo: tên/email/điện thoại, trạng thái, vai trò, ngày tạo</p>
     * <p>Hỗ trợ phân trang, sắp xếp với mặc định là createdAt giảm dần (DESC)</p>
     *
     * @param request FilterUsersRequest chứa các tiêu chí lọc (searchField, status, roles, startDate, endDate, pagination)
     * @param excludeUserId UUID của admin đang thực hiện truy vấn (để loại bỏ khỏi kết quả)
     * @return FilterUsersResponse chứa danh sách users lọc và thông tin phân trang
     * @throws BusinessException nếu search field, status, hoặc sort field không hợp lệ
     */
    com.greenconnect.greenconnect_api.dtos.response.FilterUsersResponse filterUsers(
            com.greenconnect.greenconnect_api.dtos.request.FilterUsersRequest request,
            java.util.UUID excludeUserId);

    /**
     * Lấy danh sách khách hàng (CUSTOMER role) - chỉ dành cho ADMIN và CUSTOMER_SUPPORT.
     * <p>Trả về tất cả customers với phân trang và sắp xếp</p>
     * <p>Hỗ trợ phân trang, sắp xếp với các trường: id, email, fullName, createdAt, updatedAt</p>
     *
     * @param request GetCustomersRequest chứa pageNumber, pageSize, sort, sortDirection
     * @return GetCustomersResponse chứa danh sách customers và thông tin phân trang
     * @throws BusinessException nếu sort field không hợp lệ
     */
    com.greenconnect.greenconnect_api.dtos.response.GetCustomersResponse getCustomers(
            com.greenconnect.greenconnect_api.dtos.request.GetCustomersRequest request);

    /**
     * Lọc danh sách khách hàng (CUSTOMER role) theo nhiều tiêu chí nâng cao.
     * <p>ADMIN và CUSTOMER_SUPPORT sử dụng endpoint này để lọc customers theo: tên/email/điện thoại, trạng thái, ngày tạo</p>
     * <p>Hỗ trợ phân trang, sắp xếp với mặc định là createdAt giảm dần (DESC)</p>
     *
     * @param request FilterCustomersRequest chứa các tiêu chí lọc (searchField, status, startDate, endDate, pagination)
     * @return FilterCustomersResponse chứa danh sách customers lọc và thông tin phân trang
     * @throws BusinessException nếu search field, status, hoặc sort field không hợp lệ
     */
    com.greenconnect.greenconnect_api.dtos.response.FilterCustomersResponse filterCustomers(
            com.greenconnect.greenconnect_api.dtos.request.FilterCustomersRequest request);

    /**
     * Cập nhật trạng thái tài khoản user (ACTIVE/INACTIVE).
     * <p>ADMIN và CUSTOMER_SUPPORT sử dụng để kích hoạt hoặc vô hiệu hóa tài khoản</p>
     * <p>Chỉ cho phép cập nhật ACTIVE ↔ INACTIVE</p>
     * <p>Không cho phép cập nhật sang PENDING_ACTIVATION hoặc BANNED qua endpoint này</p>
     *
     * @param email Email của user cần cập nhật trạng thái
     * @param request UpdateUserStatusRequest chứa status mới và lý do
     * @param updatedByEmail Email của admin/support thực hiện cập nhật
     * @return UpdateUserStatusResponse chứa thông tin user và trạng thái mới
     * @throws BusinessException nếu user không tồn tại hoặc status không hợp lệ
     */
    com.greenconnect.greenconnect_api.dtos.response.UpdateUserStatusResponse updateUserStatus(
            String email,
            com.greenconnect.greenconnect_api.dtos.request.UpdateUserStatusRequest request,
            String updatedByEmail);

    /**
     * Lấy danh sách sản phẩm yêu thích của user.
     * <p>Trả về List<FavoriteResponse> chứa id và productId của các sản phẩm yêu thích.</p>
     * <p>Chỉ trả về các favorite đang active (isActive=true).</p>
     *
     * @param userId UUID của user cần lấy danh sách yêu thích
     * @return List<FavoriteResponse> chứa danh sách sản phẩm yêu thích
     * @throws BusinessException nếu user không tồn tại
     */
    java.util.List<com.greenconnect.greenconnect_api.dtos.response.FavoriteResponse> getUserFavorites(
            java.util.UUID userId);

    /**
     * Reset Google Authenticator (2FA) cho user khi mất điện thoại.
     * <p>⚠️ CHỈ ADMIN mới có quyền reset 2FA cho user (check ở Controller layer)</p>
     * <p>🔄 Logic:</p>
     * <ul>
     *   <li>Tìm user theo email</li>
     *   <li>Set totp_secret = NULL</li>
     *   <li>Set is_2fa_activated = FALSE</li>
     *   <li>User sẽ phải thiết lập lại 2FA ở lần đăng nhập tiếp theo</li>
     * </ul>
     *
     * @param email Email của user cần reset 2FA
     * @return UserResponse chứa thông tin user sau khi reset
     * @throws com.greenconnect.greenconnect_api.exceptions.AppException nếu user không tồn tại
     */
    UserResponse reset2FA(String email);

}


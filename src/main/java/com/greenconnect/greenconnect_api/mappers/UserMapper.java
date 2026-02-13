package com.greenconnect.greenconnect_api.mappers;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

import com.greenconnect.greenconnect_api.dtos.request.RegisterRequest;
import com.greenconnect.greenconnect_api.dtos.request.RegisterFirebaseRequest;
import com.greenconnect.greenconnect_api.dtos.response.UserResponse;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;
import com.greenconnect.greenconnect_api.utils.PasswordUtils;

/**
 * Mapper utility class for User entity conversions.
 * <p>Chuyển đổi giữa các layers: Request DTOs → Entities → Response DTOs</p>
 * <p>Sử dụng static methods để dễ dàng import và sử dụng trong Services.</p>
 */
public final class UserMapper {
    
    // Private constructor để ngăn khởi tạo instance
    private UserMapper() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }
    
    /**
     * Chuyển đổi RegisterRequest thành User entity.
     * <p>Áp dụng các business rules:</p>
     * <ul>
     *   <li>Mã hóa password bằng BCrypt</li>
     *   <li>Set provider = LOCAL</li>
     *   <li>Set status = ACTIVE</li>
     *   <li>loyaltyPoints = 0</li>
     *   <li>Auto-generate fullName as "User" + random number (do RegisterRequest không có fullName field)</li>
     *   <li>Role sẽ được assign trong UserService</li>
     * </ul>
     *
     * @param request RegisterRequest từ client
     * @return User entity sẵn sàng để save vào database (chưa có roles)
     * @throws IllegalArgumentException nếu request null hoặc passwords không khớp
     */
    public static User fromRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RegisterRequest không được null");
        }
        
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        
        // Auto-generate fullName as "User" + random number (10000-99999)
        // Since RegisterRequest doesn't have fullName field
        String fullName = "User" + new Random().nextInt(90000) + 10000;  // Generates User10000 to User99999
        
        return User.builder()
                .email(request.getEmail().toLowerCase().trim()) // Normalize email
                .fullName(fullName) // ✅ Auto-generated fullName
                .passwordHash(PasswordUtils.hashPassword(request.getPassword()))
                .provider(Provider.LOCAL) // Local registration
                .status(UserStatus.ACTIVE) // Active by default
                .preferredPaymentMethod(com.greenconnect.greenconnect_api.enums.PaymentMethod.COD) // ✅ Mặc định COD cho customer mới
                .createdAt(LocalDateTime.now()) // ✅ Explicit set creation time
                .updatedAt(LocalDateTime.now()) // ✅ Explicit set update time
                .build();
    }
    
    /**
     * Chuyển đổi RegisterFirebaseRequest thành User entity.
     * <p>Áp dụng các business rules cho Firebase registration:</p>
     * <ul>
     *   <li>Hash Provider ID để bảo mật</li>
     *   <li>Set provider = GOOGLE (Firebase auth luôn dùng Google)</li>
     *   <li>Set status = ACTIVE</li>
     *   <li>Không có password (Firebase auth)</li>
     *   <li>Role sẽ được assign trong UserService</li>
     * </ul>
     *
     * @param request RegisterFirebaseRequest từ client
     * @return User entity sẵn sàng để save vào database (chưa có roles)
     * @throws IllegalArgumentException nếu request null
     */
    public static User fromFirebaseRequest(RegisterFirebaseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RegisterFirebaseRequest không được null");
        }
        
        return User.builder()
                .email(request.getEmail().toLowerCase().trim()) // Normalize email
                .fullName(request.getFullName() != null ? request.getFullName().trim() : "User")
                .avatarUrl(request.getPhotoURL()) // Avatar từ Firebase
                .passwordHash(null) // Firebase auth - không cần password
                .provider(Provider.GOOGLE) // ✅ Firebase luôn dùng GOOGLE provider
                .providerId(PasswordUtils.hashPassword(request.getProviderId())) // ✅ Hash Provider ID để bảo mật
                .status(UserStatus.ACTIVE) // Active by default
                .preferredPaymentMethod(com.greenconnect.greenconnect_api.enums.PaymentMethod.COD) // ✅ Mặc định COD cho customer mới
                .createdAt(LocalDateTime.now()) // ✅ Set creation time
                .updatedAt(LocalDateTime.now()) // ✅ Set update time
                .build();
    }
    
    /**
     * Chuyển đổi User entity thành UserResponse.
     * <p>Loại bỏ các thông tin nhạy cảm như passwordHash, providerId, tokens.</p>
     * <p>Trích xuất roles và primaryRole từ UserRole relationships.</p>
     *
     * @param user User entity từ database
     * @return UserResponse để trả về client
     * @throws IllegalArgumentException nếu user null
     */
    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không được null");
        }
        
        // Extract active roles from UserRole relationships
        Set<Role> roles = user.getActiveRoles();
        Role primaryRole = user.getPrimaryRole();
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider())
                .roles(roles)
                .primaryRole(primaryRole)
                .status(user.getStatus())
                .totpEnabled(user.getTotpEnabled())  // 🔐 2FA status
                .userCode(user.getUserCode())
                .loyaltyPoints(user.getLoyaltyPoints())
                .totalPaymentAmount(user.getTotalPaymentAmount())
                .nameAccountBank(user.getNameAccountBank())
                .nameBank(user.getNameBank())
                .accountNumberBank(user.getAccountNumberBank())
                .nameBankCode(user.getNameBankCode())
                .preferredPaymentMethod(user.getPreferredPaymentMethod()) // ✅ Thêm payment method
                .defaultAddress(null) // ✅ Sẽ được set riêng trong service layer
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    /**
     * Chuyển đổi User entity + Address thành UserResponse với địa chỉ mặc định.
     * <p>Overload method cho case cần kèm defaultAddress.</p>
     *
     * @param user User entity từ database
     * @param defaultAddress Address entity mặc định (isDefault=true)
     * @return UserResponse với defaultAddress
     * @throws IllegalArgumentException nếu user null
     */
    public static UserResponse toUserResponseWithAddress(User user, com.greenconnect.greenconnect_api.entities.Address defaultAddress) {
        UserResponse response = toUserResponse(user);
        
        // Map Address entity sang AddressResponse
        if (defaultAddress != null) {
            response.setDefaultAddress(AddressMapper.toAddressResponse(defaultAddress));
        }
        
        return response;
    }
}
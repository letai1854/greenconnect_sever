package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.Set;

import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho việc cập nhật thông tin người dùng.
 * <p>⭐ NGUYÊN TẮC: Tất cả các field đều OPTIONAL - chỉ update field nào khác null.</p>
 * <p>User ID sẽ được lấy từ JWT token để đảm bảo bảo mật.</p>
 * <p>Các field được map trực tiếp từ User entity để dễ maintain và linh động.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateUserRequest {
    
    // ========== PROFILE INFORMATION (User có thể tự update) ==========
    
    /**
     * Họ và tên đầy đủ của người dùng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Size(min = 2, max = 255, message = "Họ tên phải từ 2 đến 255 ký tự")
    private String fullName;
    
    /**
     * Số điện thoại của người dùng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Pattern(regexp = "^[0-9+\\-\\s()]{10,20}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
    
    /**
     * URL avatar của người dùng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Size(max = 255, message = "URL avatar không được vượt quá 255 ký tự")
    private String avatarUrl;
    
    // ========== PASSWORD MANAGEMENT ==========
    
    /**
     * Mật khẩu cũ để xác thực trước khi đổi password mới.
     * <p>Bắt buộc nếu password mới được cung cấp</p>
     */
    private String oldPassword;
    
    /**
     * Mật khẩu mới (sẽ được hash trước khi lưu).
     * <p>Optional - chỉ update nếu khác null</p>
     * <p>Nếu cung cấp, oldPassword bắt buộc phải có</p>
     */
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
    private String password;
    
    // ========== BANK INFORMATION (User có thể tự update) ==========
    
    /**
     * Tên chủ tài khoản ngân hàng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Size(max = 255, message = "Tên chủ tài khoản không được vượt quá 255 ký tự")
    private String nameAccountBank;
    
    /**
     * Tên ngân hàng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Size(max = 255, message = "Tên ngân hàng không được vượt quá 255 ký tự")
    private String nameBank;
    
    /**
     * Số tài khoản ngân hàng.
     * <p>Optional - chỉ update nếu khác null</p>
     */
    private Long accountNumberBank;
    
    /**
     * Mã ngân hàng (VD: "BIDV", "VIETCOMBANK").
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @Size(max = 255, message = "Mã ngân hàng không được vượt quá 255 ký tự")
    private String nameBankCode;
    
    // ========== PAYMENT PREFERENCES (User có thể tự update) ==========
    
    /**
     * Phương thức thanh toán ưu tiên (COD, VNPAY, MOMO, VIETQR).
     * <p>Optional - chỉ update nếu khác null</p>
     */
    private com.greenconnect.greenconnect_api.enums.PaymentMethod preferredPaymentMethod;
    
    // ========== ADMIN ONLY FIELDS (Chỉ admin có thể update) ==========
    
    /**
     * Roles của user.
     * <p>⭐ ADMIN ONLY - User bình thường không được thay đổi</p>
     * <p>Nếu không null, sẽ thay thế toàn bộ roles hiện tại.</p>
     * <p>Role đầu tiên trong Set sẽ được set làm primary role.</p>
     */
    private Set<Role> roles;
    
    /**
     * Điểm loyalty của user.
     * <p>⭐ ADMIN ONLY - User bình thường không được thay đổi</p>
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @DecimalMin(value = "0.0", message = "Điểm loyalty không được âm")
    private BigDecimal loyaltyPoints;
    
    /**
     * Status của user (ACTIVE, INACTIVE, SUSPENDED, DELETED).
     * <p>⭐ ADMIN ONLY - User bình thường không được thay đổi</p>
     * <p>Optional - chỉ update nếu khác null</p>
     */
    private UserStatus status;
    
    /**
     * Tổng tiền thanh toán của user.
     * <p>⭐ ADMIN ONLY - Thường được update tự động, nhưng admin có thể adjust</p>
     * <p>Optional - chỉ update nếu khác null</p>
     */
    @DecimalMin(value = "0.0", message = "Tổng tiền không được âm")
    private BigDecimal totalPaymentAmount;
}

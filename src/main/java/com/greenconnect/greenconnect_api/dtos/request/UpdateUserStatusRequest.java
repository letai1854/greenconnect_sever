package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.UserStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để cập nhật trạng thái tài khoản user (ACTIVE/INACTIVE).
 * <p>ADMIN và CUSTOMER_SUPPORT sử dụng để kích hoạt hoặc vô hiệu hóa tài khoản</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserStatusRequest {
    
    /**
     * Trạng thái mới của tài khoản.
     * <p>Chỉ cho phép: ACTIVE hoặc INACTIVE</p>
     * <p>⚠️ Không cho phép set PENDING_ACTIVATION hoặc BANNED qua endpoint này</p>
     */
    @NotNull(message = "Status không được để trống")
    UserStatus status;
    
    /**
     * Lý do thay đổi trạng thái (optional).
     * <p>Nên ghi rõ lý do khi vô hiệu hóa tài khoản</p>
     * <p>Ví dụ: "Vi phạm chính sách", "Yêu cầu của khách hàng", "Tài khoản spam"</p>
     */
    String reason;
}

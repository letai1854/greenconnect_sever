package com.greenconnect.greenconnect_api.enums;

/**
 * Trạng thái tài khoản người dùng trong hệ thống.
 */
public enum UserStatus {
    /**
     * Tài khoản đang hoạt động bình thường
     */
    ACTIVE,
    
    /**
     * Tài khoản bị vô hiệu hóa tạm thời (admin disabled)
     */
    INACTIVE,

    PENDING_ACTIVATION
}
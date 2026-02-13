package com.greenconnect.greenconnect_api.websocket.notification;

import java.time.LocalDateTime;
import java.util.Set;

import com.greenconnect.greenconnect_api.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho thông báo thay đổi role qua WebSocket
 * Được gửi real-time đến user khi Admin cập nhật quyền của họ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    /**
     * Loại sự kiện: "ROLE_UPDATED", "ACCOUNT_DISABLED", "FORCE_LOGOUT"
     */
    private String event;
    
    /**
     * Thông điệp chi tiết (optional)
     */
    private String message;
    
    /**
     * Roles mới (nếu là ROLE_UPDATED)
     */
    private Set<Role> newRoles;
    
    /**
     * Primary role mới
     */
    private Role newPrimaryRole;
    
    /**
     * Thời gian thông báo
     */
    private LocalDateTime timestamp;
    
    /**
     * Action mà client cần thực hiện: "REFRESH_PROFILE", "LOGOUT"
     */
    private String requiredAction;
}

package com.greenconnect.greenconnect_api.websocket.notification;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.greenconnect.greenconnect_api.enums.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý việc gửi thông báo real-time về thay đổi role qua WebSocket
 * KHÔNG liên quan đến chat service, độc lập hoàn toàn
 * 
 * Use case:
 * - Admin thay đổi role của nhân viên
 * - Hệ thống gửi thông báo đến Flutter app của nhân viên đó
 * - Flutter app tự động refresh profile và cập nhật UI
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleChangeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Gửi thông báo đến user cụ thể khi role của họ bị thay đổi
     * 
     * @param userEmail Email của user cần nhận thông báo
     * @param newRoles Set roles mới
     * @param newPrimaryRole Primary role mới
     */
    public void notifyRoleChange(String userEmail, Set<Role> newRoles, Role newPrimaryRole) {
        NotificationMessage notification = NotificationMessage.builder()
                .event("ROLE_UPDATED")
                .message("Quyền truy cập của bạn đã được cập nhật bởi quản trị viên")
                .newRoles(newRoles)
                .newPrimaryRole(newPrimaryRole)
                .timestamp(LocalDateTime.now())
                .requiredAction("REFRESH_PROFILE")
                .build();
        
        // Gửi đến kênh cá nhân của user: /user/{email}/queue/role-change
        String destination = "/queue/role-change";
        
        log.info("🔔 [ROLE CHANGE NOTIFICATION] Gửi thông báo đến user '{}' về role mới: {}", 
                userEmail, newRoles);
        
        messagingTemplate.convertAndSendToUser(
                userEmail,
                destination,
                notification
        );
        
        log.info("✅ [ROLE CHANGE NOTIFICATION] Đã gửi thông báo thành công đến user '{}'", userEmail);
    }
    
    /**
     * Gửi thông báo buộc logout (khi user bị vô hiệu hóa hoặc bị khóa tài khoản)
     * 
     * @param userEmail Email của user
     * @param reason Lý do buộc logout
     */
    public void notifyForceLogout(String userEmail, String reason) {
        NotificationMessage notification = NotificationMessage.builder()
                .event("FORCE_LOGOUT")
                .message(reason)
                .timestamp(LocalDateTime.now())
                .requiredAction("LOGOUT")
                .build();
        
        log.info("🚨 [FORCE LOGOUT] Gửi thông báo logout đến user '{}': {}", userEmail, reason);
        
        messagingTemplate.convertAndSendToUser(
                userEmail,
                "/queue/role-change",
                notification
        );
        
        log.info("✅ [FORCE LOGOUT] Đã gửi thông báo logout thành công đến user '{}'", userEmail);
    }
    
    /**
     * Gửi thông báo khi tài khoản bị vô hiệu hóa
     * 
     * @param userEmail Email của user
     */
    public void notifyAccountDisabled(String userEmail) {
        notifyForceLogout(userEmail, "Tài khoản của bạn đã bị vô hiệu hóa bởi quản trị viên");
    }
}

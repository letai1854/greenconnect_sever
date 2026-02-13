package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.NotificationRecipient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho Notification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private UUID id;
    private UUID userId;
    private Long userCode; // Mã code của user gửi thông báo
    private String type;
    private String title;
    private String message;
    private String link;
    private NotificationRecipient recipient;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

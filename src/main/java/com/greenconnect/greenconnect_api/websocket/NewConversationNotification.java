package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO để thông báo về cuộc hội thoại mới được tạo
 * Gửi đến các nhân viên hỗ trợ và admin qua WebSocket
 */
@Data
@Builder
public class NewConversationNotification {
    
    /**
     * ID của cuộc hội thoại mới
     */
    private UUID conversationId;
    
    /**
     * Thông tin khách hàng tạo cuộc hội thoại
     */
    private String customerName;
    private String customerEmail;
    
    /**
     * Tin nhắn đầu tiên của khách hàng
     */
    private String firstMessage;
    
    /**
     * Thời gian tạo cuộc hội thoại
     */
    private LocalDateTime createdAt;
    
    /**
     * Loại thông báo
     */
    @Builder.Default
    private String notificationType = "NEW_CONVERSATION";
    
    /**
     * Trạng thái cuộc hội thoại
     */
    @Builder.Default
    private String status = "NEW";
}
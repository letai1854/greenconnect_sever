package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho danh sách conversations với thông tin tóm tắt
 * Sử dụng cho API lấy danh sách conversations với phân trang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationListItemResponse {
    
    /**
     * ID của cuộc trò chuyện
     */
    private UUID conversationId;
    
    /**
     * Thông tin khách hàng
     */
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerAvatarUrl;
    
    /**
     * Thông tin nhân viên được phân công (có thể null)
     */
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeRole;
    
    /**
     * Thông tin cuộc trò chuyện
     */
    private String title;
    private String status;
    
    /**
     * Tin nhắn cuối cùng
     */
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private String lastSenderName;
    private String lastSenderRole;
    
    /**
     * Thống kê
     */
    private Integer totalMessages;
    private Integer unreadMessages;
    
    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
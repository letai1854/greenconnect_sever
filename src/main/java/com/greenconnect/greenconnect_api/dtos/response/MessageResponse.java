package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.MessageStatus;
import com.greenconnect.greenconnect_api.enums.MessageType;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class MessageResponse {
    private UUID messageId;
    
    /**
     * ID tạm thời từ client (để client mapping với tin nhắn PENDING)
     */
    private String tempMessageId;
    
    private UUID senderId;
    private String senderName;
    private String senderRole;
    private MessageType messageType;
    private String content;
    private String mediaUrl;
    private LocalDateTime createdAt;
    
    /**
     * Trạng thái tin nhắn: SENT hoặc FAILED
     */
    private MessageStatus status;
    
    /**
     * Thông báo lỗi (nếu status = FAILED)
     */
    private String errorMessage;
}

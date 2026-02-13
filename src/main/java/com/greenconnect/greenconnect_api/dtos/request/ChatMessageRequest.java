package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.MessageType;

import lombok.Data;

@Data
public class ChatMessageRequest {
    /**
     * ID tạm thời do client tạo để track tin nhắn (UUID ngẫu nhiên)
     * Dùng để map response về đúng tin nhắn đang PENDING trên UI
     */
    private String tempMessageId;
    
    private String content;
    private String mediaUrl;
    private MessageType messageType;
}

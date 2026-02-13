package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.dtos.request.ChatMessageRequest;
import com.greenconnect.greenconnect_api.dtos.response.MessageResponse;
import com.greenconnect.greenconnect_api.entities.Message;
import com.greenconnect.greenconnect_api.enums.MessageStatus;
import com.greenconnect.greenconnect_api.enums.MessageType;

@Component
public class MessageMapper {

    public Message toMessage(ChatMessageRequest request) {
        if (request == null) {
            return null;
        }

        return Message.builder()
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .messageType(request.getMessageType() != null ? request.getMessageType() : determineMessageType(request))
                .status(MessageStatus.SENT) // Mặc định SENT khi tạo tin nhắn
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .build();
    }

    public MessageResponse toMessageResponse(Message message) {
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
                .messageId(message.getId())
                .content(message.getContent())
                .mediaUrl(message.getMediaUrl())
                .messageType(message.getMessageType())
                .status(message.getStatus()) // Thêm status từ entity
                .senderName(message.getSender() != null ? message.getSender().getFullName() : "Unknown")
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderRole(message.getSender() != null ? message.getSender().getPrimaryRole().name() : "UNKNOWN")
                .createdAt(message.getCreatedAt())
                .build();
    }

    private MessageType determineMessageType(ChatMessageRequest request) {
        if (request.getMediaUrl() != null && !request.getMediaUrl().trim().isEmpty()) {
            return MessageType.IMAGE;
        }
        return MessageType.TEXT;
    }
}
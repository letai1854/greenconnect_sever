package com.greenconnect.greenconnect_api.websocket;

import java.security.Principal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.greenconnect.greenconnect_api.dtos.request.ChatMessageRequest;
import com.greenconnect.greenconnect_api.dtos.response.MessageResponse;
import com.greenconnect.greenconnect_api.enums.MessageStatus;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class ChatController {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send/{conversationId}")
    public void handleMessage(
            @DestinationVariable UUID conversationId,
            @Payload ChatMessageRequest incomingMessage,
            Principal principal
    ) {
        if (principal == null) { 
            log.warn("⚠️ [NO AUTH] Không có thông tin người dùng");
            return; 
        }
        
        String userEmail = principal.getName();
        
        try {
            // ⭐ Xử lý và broadcast tin nhắn
            chatService.processAndBroadcastMessage(conversationId, incomingMessage, userEmail);
            
        } catch (Exception e) {
            log.error("❌ [SEND ERROR] Lỗi khi xử lý tin nhắn từ {}: {}", userEmail, e.getMessage(), e);
            
            // ⭐ GỬI THÔNG BÁO LỖI RIÊNG CHỜ NGƯỜI GỬI
            MessageResponse errorResponse = MessageResponse.builder()
                    .tempMessageId(incomingMessage.getTempMessageId())
                    .status(MessageStatus.FAILED)
                    .errorMessage("Không thể gửi tin nhắn: " + e.getMessage())
                    .build();
            
            // Gửi đến queue riêng của user để client hiển thị lỗi
            messagingTemplate.convertAndSendToUser(
                userEmail, 
                "/queue/errors", 
                errorResponse
            );
            
            log.info("📤 [ERROR SENT] Đã gửi thông báo lỗi đến user: {}", userEmail);
        }
    }

    /**
     * ⭐ Xử lý khi user đánh dấu đã đọc qua WebSocket
     * Client gửi: /app/chat.markRead/{conversationId}
     */
    @MessageMapping("/chat.markRead/{conversationId}")
    public void handleMarkAsRead(
            @DestinationVariable UUID conversationId,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("⚠️ [NO AUTH] Không có thông tin người dùng khi mark as read");
            return;
        }
        
        String userEmail = principal.getName();
        
        try {
            log.info("📖 [MARK READ WS] User '{}' đánh dấu đã đọc conversation '{}'", userEmail, conversationId);
            
            // Cập nhật database
            chatService.markConversationAsRead(conversationId, userEmail);
            
            log.info("✅ [MARK READ WS] Đã đánh dấu conversation '{}' là đã đọc cho user '{}'", 
                    conversationId, userEmail);
            
        } catch (Exception e) {
            log.error("❌ [MARK READ ERROR] Lỗi khi đánh dấu đã đọc: {}", e.getMessage(), e);
        }
    }
}
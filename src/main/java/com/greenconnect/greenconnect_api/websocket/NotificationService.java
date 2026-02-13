package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.greenconnect.greenconnect_api.entities.Conversation;
import com.greenconnect.greenconnect_api.entities.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service chuyên xử lý thông báo WebSocket
 * Tách biệt logic thông báo khỏi ChatService chính
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Kênh thông báo chung cho tất cả nhân viên hỗ trợ và admin
     */
    private static final String SUPPORT_NOTIFICATION_TOPIC = "/topic/support/notifications";
    
    /**
     * Gửi thông báo về cuộc hội thoại mới đến tất cả nhân viên hỗ trợ
     * 
     * @param conversation Cuộc hội thoại mới được tạo
     * @param firstMessage Tin nhắn đầu tiên
     */
    public void notifyNewConversation(Conversation conversation, String firstMessage) {
        try {
            User customer = conversation.getCustomer();
            
            // ⭐ FALLBACK: Nếu createdAt vẫn null, dùng thời gian hiện tại
            LocalDateTime createdAt = conversation.getCreatedAt();
            if (createdAt == null) {
                log.warn("⚠️ [FALLBACK] CreatedAt is null for conversation {}, using current time", conversation.getId());
                createdAt = LocalDateTime.now();
            }
            
            // Tạo thông báo
            NewConversationNotification notification = NewConversationNotification.builder()
                    .conversationId(conversation.getId())
                    .customerName(customer.getFullName())
                    .customerEmail(customer.getEmail())
                    .firstMessage(firstMessage)
                    .createdAt(createdAt)  // ✅ Đảm bảo không bao giờ null
                    .build();
            
            // Log trước khi gửi
            log.info("📢 [NEW CONVERSATION NOTIFICATION] Preparing to broadcast:");
            log.info("   ├── Conversation ID: {}", conversation.getId());
            log.info("   ├── Customer: {} ({})", customer.getFullName(), customer.getEmail());
            log.info("   ├── First Message: {}", firstMessage);
            log.info("   ├── Topic: {}", SUPPORT_NOTIFICATION_TOPIC);
            log.info("   └── Created At: {}", createdAt);
            
            // Gửi thông báo
            messagingTemplate.convertAndSend(SUPPORT_NOTIFICATION_TOPIC, notification);
            
            // Log sau khi gửi thành công
            log.info("✅ [BROADCAST SUCCESS] New conversation notification sent successfully");
            log.info("   └── All support agents and admins should receive this notification");
            
        } catch (Exception e) {
            log.error("❌ [BROADCAST ERROR] Failed to send new conversation notification:", e);
            log.error("   ├── Conversation ID: {}", conversation.getId());
            log.error("   └── Error: {}", e.getMessage());
        }
    }
    
    /**
     * Gửi thông báo tùy chỉnh đến topic cụ thể
     * 
     * @param topic Kênh thông báo
     * @param payload Nội dung thông báo
     */
    public void sendCustomNotification(String topic, Object payload) {
        try {
            log.info("📤 [CUSTOM NOTIFICATION] Sending to topic: {}", topic);
            messagingTemplate.convertAndSend(topic, payload);
            log.info("✅ [BROADCAST SUCCESS] Custom notification sent successfully");
        } catch (Exception e) {
            log.error("❌ [BROADCAST ERROR] Failed to send custom notification to {}: {}", topic, e.getMessage());
        }
    }
}
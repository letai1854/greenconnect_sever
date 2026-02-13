package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.dtos.response.ConversationListItemResponse;
import com.greenconnect.greenconnect_api.entities.Conversation;
import com.greenconnect.greenconnect_api.entities.Message;
import com.greenconnect.greenconnect_api.entities.User;

/**
 * Mapper để convert giữa Conversation entity và các DTO response
 * Tách biệt logic mapping khỏi service layer
 */
@Component
public class ConversationMapper {

    /**
     * Convert Conversation entity sang ConversationListItemResponse
     * Bao gồm thông tin tóm tắt cho danh sách conversations
     */
    public ConversationListItemResponse toConversationListItem(Conversation conversation) {
        if (conversation == null) {
            return null;
        }

        // Lấy thông tin khách hàng
        User customer = conversation.getCustomer();
        
        // Lấy thông tin assignee (có thể null)
        User assignee = conversation.getAssignee();
        
        // Tìm tin nhắn cuối cùng (giả sử messages được sắp xếp theo createdAt DESC)
        Message lastMessage = null;
        if (conversation.getMessages() != null && !conversation.getMessages().isEmpty()) {
            lastMessage = conversation.getMessages().get(0); // Tin nhắn đầu tiên trong list đã sort DESC
        }

        ConversationListItemResponse.ConversationListItemResponseBuilder builder = ConversationListItemResponse.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt());

        // Thông tin khách hàng
        if (customer != null) {
            builder.customerId(customer.getId())
                   .customerName(customer.getFullName())
                   .customerEmail(customer.getEmail())
                   .customerAvatarUrl(customer.getAvatarUrl());
        }

        // Thông tin assignee (có thể null)
        if (assignee != null) {
            builder.assigneeId(assignee.getId())
                   .assigneeName(assignee.getFullName())
                   .assigneeRole(assignee.getPrimaryRole().name());
        }

        // Thông tin tin nhắn cuối cùng
        if (lastMessage != null) {
            builder.lastMessageContent(lastMessage.getContent())
                   .lastMessageTime(lastMessage.getCreatedAt());
            
            if (lastMessage.getSender() != null) {
                builder.lastSenderName(lastMessage.getSender().getFullName())
                       .lastSenderRole(lastMessage.getSender().getPrimaryRole().name());
            }
        }

        // Thống kê (tạm thời hardcode, có thể implement sau)
        if (conversation.getMessages() != null) {
            builder.totalMessages(conversation.getMessages().size());
            // Đếm unread messages (giả sử có field isRead)
            long unreadCount = conversation.getMessages().stream()
                    .filter(msg -> !msg.getIsRead())
                    .count();
            builder.unreadMessages((int) unreadCount);
        } else {
            builder.totalMessages(0)
                   .unreadMessages(0);
        }

        return builder.build();
    }

    /**
     * Convert Conversation entity sang ConversationListItemResponse (phiên bản tối ưu)
     * Không load messages để tránh N+1 query problem
     * Thông tin lastMessage và statistics sẽ được truyền vào từ ngoài
     */
    public ConversationListItemResponse toConversationListItem(
            Conversation conversation, 
            String lastMessageContent,
            LocalDateTime lastMessageTime,
            String lastSenderName,
            String lastSenderRole,
            Integer totalMessages,
            Integer unreadMessages) {
        
        if (conversation == null) {
            return null;
        }

        User customer = conversation.getCustomer();
        User assignee = conversation.getAssignee();

        ConversationListItemResponse.ConversationListItemResponseBuilder builder = ConversationListItemResponse.builder()
                .conversationId(conversation.getId())
                .title(conversation.getTitle())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessageContent(lastMessageContent)
                .lastMessageTime(lastMessageTime)
                .lastSenderName(lastSenderName)
                .lastSenderRole(lastSenderRole)
                .totalMessages(totalMessages != null ? totalMessages : 0)
                .unreadMessages(unreadMessages != null ? unreadMessages : 0);

        // Thông tin khách hàng
        if (customer != null) {
            builder.customerId(customer.getId())
                   .customerName(customer.getFullName())
                   .customerEmail(customer.getEmail())
                   .customerAvatarUrl(customer.getAvatarUrl());
        }

        // Thông tin assignee
        if (assignee != null) {
            builder.assigneeId(assignee.getId())
                   .assigneeName(assignee.getFullName())
                   .assigneeRole(assignee.getPrimaryRole().name());
        }

        return builder.build();
    }
}
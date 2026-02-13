package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.greenconnect.greenconnect_api.enums.MessageStatus;
import com.greenconnect.greenconnect_api.enums.MessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * Đại diện cho một tin nhắn duy nhất trong một cuộc trò chuyện.
 */
@Entity
@Table(name = "messages", indexes = {
    @jakarta.persistence.Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at"),
    @jakarta.persistence.Index(name = "idx_message_sender", columnList = "sender_id"),
    // ✅ NEW: Unread messages for user
    @jakarta.persistence.Index(name = "idx_message_conversation_read", columnList = "conversation_id, is_read"),
    // ✅ NEW: Message status tracking
    @jakarta.persistence.Index(name = "idx_message_status", columnList = "status"),
    // ✅ NEW: Sender messages timeline
    @jakarta.persistence.Index(name = "idx_message_sender_created", columnList = "sender_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Nội dung text, có thể là NULL

    @Column(name = "media_url", length = 255)
    private String mediaUrl; // URL của ảnh/video, có thể là NULL
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    /**
     * Trạng thái gửi tin nhắn: SENT (đã lưu DB), FAILED (lỗi)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
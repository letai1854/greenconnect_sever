package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ChatMessage - Lưu từng tin nhắn trong cuộc hội thoại
 * Bao gồm tin nhắn của USER và BOT
 */
@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageSender sender; // USER hoặc BOT

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // Nội dung tin nhắn

    /**
     * Lưu danh sách UUID sản phẩm được gợi ý (format: uuid1,uuid2,uuid3)
     * Frontend dùng để fetch thông tin sản phẩm mới nhất
     * Chỉ có khi sender = BOT
     */
    @Column(name = "reference_product_ids", columnDefinition = "TEXT")
    private String referenceProductIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageSender {
        USER,   // Tin nhắn từ user
        BOT     // Tin nhắn từ AI chatbot
    }
}

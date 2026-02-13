package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ChatSession - Quản lý các cuộc hội thoại chat với AI
 * Mỗi user có thể có nhiều session (giống ChatGPT)
 */
@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 255)
    private String title; // Tự động đặt tên hoặc user đặt (VD: "Hỏi về thịt gà")

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Cập nhật khi có tin nhắn mới (để sort)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Để user có thể "delete" (soft delete)
}

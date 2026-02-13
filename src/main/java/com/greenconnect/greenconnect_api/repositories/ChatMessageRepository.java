package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.ChatMessage;
import com.greenconnect.greenconnect_api.entities.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    /**
     * Lấy tin nhắn của session theo thứ tự mới nhất trước (DESC)
     * ⚠️ KHÔNG dùng @Query custom - để Spring Data JPA tự generate
     * ✅ Pageable sẽ tự động apply LIMIT + OFFSET đúng cách
     * 
     * @param sessionId Session ID
     * @param pageable Pagination info (page, size)
     * @return List tin nhắn (đã phân trang, ORDER BY createdAt DESC)
     */
    List<ChatMessage> findBySession_IdOrderByCreatedAtDesc(
        UUID sessionId, 
        org.springframework.data.domain.Pageable pageable
    );
    
    /**
     * Đếm số tin nhắn trong session
     */
    long countBySession(ChatSession session);
}

package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    
    /**
     * Lấy tất cả session của user (sắp xếp theo updatedAt DESC)
     */
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(UUID userId);
    
    /**
     * Đếm số session của user
     */
    long countByUserIdAndIsActiveTrue(UUID userId);
}

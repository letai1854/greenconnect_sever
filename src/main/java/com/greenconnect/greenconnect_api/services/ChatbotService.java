package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.ChatRequest;
import com.greenconnect.greenconnect_api.dtos.response.ChatHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatSessionListResponse;

import java.util.UUID;

public interface ChatbotService {
    
    /**
     * Xử lý câu hỏi từ user, gửi lên Gemini AI và trả về câu trả lời kèm sản phẩm gợi ý
     * 
     * @param request ChatRequest chỉ chứa userQuery
     * @param userId User ID từ JWT token
     * @return ChatResponse chứa bot_message và danh sách products
     */
    ChatResponse processChat(ChatRequest request, UUID userId);
    
    /**
     * Lấy session hiện tại của user (cho e-commerce chatbot)
     * Trả về session active hoặc tạo mới nếu chưa có
     * 
     * @param userId User ID
     * @return UUID của session
     */
    UUID getCurrentSession(UUID userId);
    
    /**
     * Lấy danh sách tất cả session của user (để hiển thị sidebar)
     * ⚠️ Deprecated cho e-commerce chatbot (vì chỉ có 1 session duy nhất)
     * 
     * @param userId User ID
     * @return Danh sách session (sorted by updatedAt DESC)
     */
    ChatSessionListResponse getUserSessions(UUID userId);
    
    /**
     * Load lịch sử chat của 1 session cụ thể (kèm product details) với pagination
     * 
     * @param sessionId Session ID
     * @param userId User ID (để verify ownership)
     * @param page Page number (0-based)
     * @param size Page size
     * @return Lịch sử chat với product details
     */
    ChatHistoryResponse getSessionHistory(UUID sessionId, UUID userId, int page, int size);
    
    /**
     * Xóa session (soft delete - set isActive = false)
     * 
     * @param sessionId Session ID
     * @param userId User ID (để verify ownership)
     */
    void deleteSession(UUID sessionId, UUID userId);
}

package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.ChatRequest;
import com.greenconnect.greenconnect_api.dtos.response.ChatHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatSessionListResponse;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ai_controller {
    
    private final ChatbotService chatbotService;
    
    /**
     * Lấy userId từ JWT token (CustomUserPrincipal)
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        // ✅ Lấy từ CustomUserPrincipal (không phải email!)
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal) {
            return ((CustomUserPrincipal) principal).getUserId();
        }
        
        throw new RuntimeException("Invalid authentication principal");
    }
    
    /**
     * Endpoint xử lý chat với AI chatbot
     * ✅ Frontend chỉ gửi: userQuery
     * ✅ Backend tự lấy userId từ JWT token
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @param request ChatRequest chỉ chứa userQuery
     * @return ChatResponse chứa bot_message và danh sách sản phẩm gợi ý
     */
    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ChatResponse> askChatbot(@RequestBody ChatRequest request) {
        UUID userId = getCurrentUserId();
        log.info("📨 Received chat request from user: {}", userId);
        
        ChatResponse response = chatbotService.processChat(request, userId);
        
        log.info("✅ Chat response prepared with {} suggested products", 
                response.getProducts() != null ? response.getProducts().size() : 0);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy session hiện tại của user (cho e-commerce chatbot)
     * ✅ Frontend không cần gửi userId (lấy từ JWT token)
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @return UUID của session hiện tại
     */
    @GetMapping("/current-session")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UUID> getCurrentSession() {
        UUID userId = getCurrentUserId();
        log.info("🔍 Getting current session for user: {}", userId);
        
        UUID sessionId = chatbotService.getCurrentSession(userId);
        
        log.info("✅ Current session: {}", sessionId);
        
        return ResponseEntity.ok(sessionId);
    }
    
    /**
     * Lấy danh sách session của user (để hiển thị sidebar)
     * ⚠️ Deprecated - E-commerce chatbot chỉ có 1 session duy nhất
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @return Danh sách session
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ChatSessionListResponse> getUserSessions() {
        UUID userId = getCurrentUserId();
        log.info("📋 Getting sessions for user: {}", userId);
        
        ChatSessionListResponse response = chatbotService.getUserSessions(userId);
        
        log.info("✅ Found {} sessions", response.getSessions().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Load lịch sử chat của session hiện tại (kèm product details)
     * ✅ Backend tự tìm session của user (không cần frontend gửi sessionId)
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @param page Page number (0-based, default = 0)
     * @param size Page size (default = 20)
     * @return Lịch sử chat với product details
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ChatHistoryResponse> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        log.info("📜 Getting history for user: {} (page={}, size={})", userId, page, size);
        
        // Backend tự tìm session active của user
        UUID sessionId = chatbotService.getCurrentSession(userId);
        
        ChatHistoryResponse response = chatbotService.getSessionHistory(sessionId, userId, page, size);
        
        log.info("✅ Loaded {} messages", response.getMessages().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Load lịch sử chat của 1 session cụ thể (kèm product details)
     * ⚠️ Legacy endpoint - Giữ lại cho compatibility
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @param sessionId Session ID
     * @param page Page number (0-based, default = 0)
     * @param size Page size (default = 20)
     * @return Lịch sử chat với product details
     */
    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ChatHistoryResponse> getSessionHistory(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        log.info("📜 Getting history for session: {} (page={}, size={})", sessionId, page, size);
        
        ChatHistoryResponse response = chatbotService.getSessionHistory(sessionId, userId, page, size);
        
        log.info("✅ Loaded {} messages", response.getMessages().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa session hiện tại (soft delete)
     * ⚠️ Nguy hiểm - Xóa toàn bộ lịch sử chat
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     */
    @DeleteMapping("/session")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deleteCurrentSession() {
        UUID userId = getCurrentUserId();
        log.info("🗑️ Deleting current session for user: {}", userId);
        
        UUID sessionId = chatbotService.getCurrentSession(userId);
        chatbotService.deleteSession(sessionId, userId);
        
        log.info("✅ Session deleted");
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Xóa session cụ thể (soft delete)
     * ⚠️ Legacy endpoint - Giữ lại cho compatibility
     * ✅ Yêu cầu role CUSTOMER hoặc ADMIN
     * 
     * @param sessionId Session ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId) {
        UUID userId = getCurrentUserId();
        log.info("🗑️ Deleting session: {}", sessionId);
        
        chatbotService.deleteSession(sessionId, userId);
        
        log.info("✅ Session deleted");
        
        return ResponseEntity.noContent().build();
    }
}

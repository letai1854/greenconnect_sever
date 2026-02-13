package com.greenconnect.greenconnect_api.websocket;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.StartConversationRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ConversationListItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.ConversationResponse;
import com.greenconnect.greenconnect_api.dtos.response.MessageResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal; // ⭐ THÊM IMPORT

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/conversations") 
@RequiredArgsConstructor
@Slf4j
public class ChatApiController {

    private final ChatService chatService;

    @PostMapping("/start")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(
            @AuthenticationPrincipal CustomUserPrincipal currentUser, // ⭐ SỬA LẠI
            @Valid @RequestBody StartConversationRequest request
    ) {
        log.info("API: Người dùng '{}' bắt đầu cuộc trò chuyện mới.", currentUser.getEmail());
        String customerEmail = currentUser.getEmail(); // ⭐ SỬA LẠI
        ConversationResponse conversation = chatService.startConversation(customerEmail, request.getFirstMessage());
        
        ApiResponse<ConversationResponse> apiResponse = ResponseUtil.success(
            conversation, 
            "Tạo cuộc trò chuyện thành công"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("@chatSecurityService.canAccessConversation(#conversationId)") // Sử dụng ChatSecurityService để kiểm tra quyền truy cập
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getConversationMessages(
            @AuthenticationPrincipal CustomUserPrincipal currentUser, // ⭐ SỬA LẠI
            @PathVariable UUID conversationId,
            Pageable pageable
    ) {
        log.info("API: Người dùng '{}' lấy tin nhắn cho cuộc trò chuyện '{}'", currentUser.getEmail(), conversationId);
        String userEmail = currentUser.getEmail(); // ⭐ SỬA LẠI
        Page<MessageResponse> messagesPage = chatService.getMessagesForConversation(conversationId, userEmail, pageable);
        
        ApiResponse<Page<MessageResponse>> apiResponse = ResponseUtil.success(
            messagesPage, 
            "Lấy lịch sử tin nhắn thành công"
        );
        
        return ResponseEntity.ok(apiResponse); 
    }

    // ================= ⭐ CÁC ENDPOINT MỚI BỔ SUNG ⭐ =================

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<ConversationListItemResponse>>> getAllConversations(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            Pageable pageable
    ) {
        log.info("API: Admin/Support '{}' đang lấy danh sách tất cả conversations", currentUser.getEmail());
        
        Page<ConversationListItemResponse> conversationsPage = chatService.getAllConversations(
                currentUser.getEmail(), 
                pageable
        );
        
        ApiResponse<Page<ConversationListItemResponse>> apiResponse = ResponseUtil.success(
            conversationsPage, 
            "Lấy danh sách cuộc trò chuyện thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<ConversationListItemResponse>>> getConversationsByStatus(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @PathVariable String status,
            Pageable pageable
    ) {
        log.info("API: Admin/Support '{}' đang lấy conversations với status '{}'", currentUser.getEmail(), status);
        
        Page<ConversationListItemResponse> conversationsPage = chatService.getConversationsByStatus(
                status,
                currentUser.getEmail(), 
                pageable
        );
        
        ApiResponse<Page<ConversationListItemResponse>> apiResponse = ResponseUtil.success(
            conversationsPage, 
            String.format("Lấy danh sách cuộc trò chuyện với trạng thái '%s' thành công", status)
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<ConversationListItemResponse>>> getMyConversations(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            Pageable pageable
    ) {
        log.info("API: Customer '{}' đang lấy danh sách conversations của mình", currentUser.getEmail());
        
        Page<ConversationListItemResponse> conversationsPage = chatService.getMyConversations(
                currentUser.getEmail(), 
                pageable
        );
        
        ApiResponse<Page<ConversationListItemResponse>> apiResponse = ResponseUtil.success(
            conversationsPage, 
            "Lấy danh sách cuộc trò chuyện của bạn thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

        @GetMapping("/assigned")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<ConversationListItemResponse>>> getAssignedConversations(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            Pageable pageable
    ) {
        log.info("API: Agent '{}' đang lấy conversations được assign", currentUser.getEmail());
        
        Page<ConversationListItemResponse> conversationsPage = chatService.getAssignedConversations(
                currentUser.getEmail(), 
                pageable
        );
        
        ApiResponse<Page<ConversationListItemResponse>> apiResponse = ResponseUtil.success(
            conversationsPage, 
            "Lấy danh sách cuộc trò chuyện được phân công thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * ⭐ SEARCH: Tìm kiếm conversations theo tên customer
     * Dành cho admin và customer support để tìm kiếm cuộc trò chuyện
     * @param searchTerm Từ khóa tìm kiếm (tên hoặc email của customer)
     * @param pageable Thông tin phân trang
     * @return Danh sách conversations phù hợp với từ khóa tìm kiếm
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<ConversationListItemResponse>>> searchConversations(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @RequestParam String searchTerm,
            Pageable pageable
    ) {
        log.info("API: Admin/Support '{}' đang tìm kiếm conversations với từ khóa '{}'", 
                currentUser.getEmail(), searchTerm);
        
        Page<ConversationListItemResponse> conversationsPage = chatService.searchConversations(
                searchTerm,
                currentUser.getEmail(), 
                pageable
        );
        
        ApiResponse<Page<ConversationListItemResponse>> apiResponse = ResponseUtil.success(
            conversationsPage, 
            String.format("Tìm thấy cuộc trò chuyện với từ khóa '%s'", searchTerm)
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
        /**
     * ⭐ BỔ SUNG: Lấy conversationId của user theo email
     * Dùng để lấy conversationId hiện tại của user khi cần chat
     */
    @GetMapping("/user/{email:.+}")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConversationResponse>> getUserConversation(
            @PathVariable String email
    ) {
        log.info("API: Đang lấy conversationId của user '{}'", email);
        
        ConversationResponse conversation = chatService.getUserConversation(email);
        
        ApiResponse<ConversationResponse> apiResponse = ResponseUtil.success(
            conversation, 
            "Lấy conversationId của người dùng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    // ================= ⭐ MARK AS READ ENDPOINTS ⭐ =================

    /**
     * ⭐ Đánh dấu tất cả tin nhắn trong conversation là đã đọc
     */
    @PostMapping("/{conversationId}/mark-read")
    @PreAuthorize("@chatSecurityService.canAccessConversation(#conversationId)")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @PathVariable UUID conversationId
    ) {
        log.info("API: User '{}' đánh dấu đã đọc conversation '{}'", currentUser.getEmail(), conversationId);
        
        chatService.markConversationAsRead(conversationId, currentUser.getEmail());
        
        ApiResponse<Void> apiResponse = ResponseUtil.success(
            null, 
            "Đã đánh dấu tất cả tin nhắn là đã đọc"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * ⭐ Đánh dấu một tin nhắn cụ thể là đã đọc
     */
    @PostMapping("/messages/{messageId}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @PathVariable UUID messageId
    ) {
        log.info("API: User '{}' đánh dấu đã đọc message '{}'", currentUser.getEmail(), messageId);
        
        chatService.markMessageAsRead(messageId, currentUser.getEmail());
        
        ApiResponse<Void> apiResponse = ResponseUtil.success(
            null, 
            "Đã đánh dấu tin nhắn là đã đọc"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * ⭐ Đếm số tin nhắn chưa đọc trong một conversation
     */
    @GetMapping("/{conversationId}/unread-count")
    @PreAuthorize("@chatSecurityService.canAccessConversation(#conversationId)")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @PathVariable UUID conversationId
    ) {
        log.info("API: User '{}' đếm tin nhắn chưa đọc trong conversation '{}'", 
                currentUser.getEmail(), conversationId);
        
        long count = chatService.countUnreadMessages(conversationId, currentUser.getEmail());
        
        ApiResponse<Long> apiResponse = ResponseUtil.success(
            count, 
            String.format("Có %d tin nhắn chưa đọc", count)
        );
        
        return ResponseEntity.ok(apiResponse);
    }
}
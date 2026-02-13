package com.greenconnect.greenconnect_api.websocket;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.ChatMessageRequest;
import com.greenconnect.greenconnect_api.dtos.response.ConversationListItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.ConversationResponse;
import com.greenconnect.greenconnect_api.dtos.response.MessageResponse;

public interface ChatService {

    /**
     * Khách hàng bắt đầu một cuộc trò chuyện mới.
     * @param customerEmail Email của khách hàng.
     * @param firstMessage Nội dung tin nhắn đầu tiên.
     * @return Thông tin về cuộc trò chuyện vừa được tạo.
     */
    ConversationResponse startConversation(String customerEmail, String firstMessage);

    /**
     * Lấy lịch sử tin nhắn của một cuộc trò chuyện, có phân trang.
     * @param conversationId ID của cuộc trò chuyện.
     * @param userEmail Email của người dùng đang yêu cầu (để kiểm tra quyền).
     * @param pageable Thông tin phân trang.
     * @return Một trang (Page) chứa danh sách các tin nhắn.
     */
    Page<MessageResponse> getMessagesForConversation(UUID conversationId, String userEmail, Pageable pageable);

    /**
     * Xử lý một tin nhắn đến qua WebSocket.
     * @param conversationId ID của cuộc trò chuyện.
     * @param messageRequest DTO chứa nội dung tin nhắn.
     * @param senderEmail Email của người gửi.
     */
    void processAndBroadcastMessage(UUID conversationId, ChatMessageRequest messageRequest, String senderEmail);

    /**
     * ⭐ BỔ SUNG: Lấy danh sách tất cả conversations với phân trang
     * Dành cho admin và support agent xem tổng quan tất cả cuộc trò chuyện
     * @param userEmail Email của người dùng đang yêu cầu (để kiểm tra quyền)
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations
     */
    Page<ConversationListItemResponse> getAllConversations(String userEmail, Pageable pageable);

    /**
     * ⭐ BỔ SUNG: Lấy danh sách conversations theo trạng thái với phân trang  
     * Ví dụ: chỉ lấy conversations có status = "NEW", "OPEN", etc.
     * @param status Trạng thái conversation cần lọc
     * @param userEmail Email của người dùng đang yêu cầu (để kiểm tra quyền)
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations theo trạng thái
     */
    Page<ConversationListItemResponse> getConversationsByStatus(String status, String userEmail, Pageable pageable);

    /**
     * ⭐ BỔ SUNG: Lấy danh sách conversations của khách hàng hiện tại
     * Dùng khi khách hàng muốn xem lịch sử các cuộc trò chuyện của mình
     * @param userEmail Email của khách hàng
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations của khách hàng
     */
    Page<ConversationListItemResponse> getMyConversations(String userEmail, Pageable pageable);

    /**
     * ⭐ BỔ SUNG: Lấy danh sách conversations được assign cho nhân viên hiện tại
     * Dùng khi nhân viên muốn xem những conversation được giao cho mình
     * @param userEmail Email của nhân viên
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations được assign cho nhân viên
     */
    Page<ConversationListItemResponse> getAssignedConversations(String userEmail, Pageable pageable);
    
    /**
     * ⭐ SEARCH: Tìm kiếm conversations theo tên hoặc email của customer
     * Dành cho admin và customer support để tìm kiếm cuộc trò chuyện
     * @param searchTerm Từ khóa tìm kiếm (tên hoặc email của customer)
     * @param userEmail Email của người dùng đang yêu cầu (để kiểm tra quyền)
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations phù hợp với từ khóa tìm kiếm
     */
    Page<ConversationListItemResponse> searchConversations(String searchTerm, String userEmail, Pageable pageable);
    
    /**
     * ⭐ BỔ SUNG: Lấy conversation hiện tại của một user theo email
     * Dùng khi cần lấy conversationId để bắt đầu hoặc tiếp tục chat
     * @param email Email của khách hàng
     * @return Thông tin conversation mới nhất của user
     */
    ConversationResponse getUserConversation(String email);

    /**
     * ⭐ Đánh dấu tất cả tin nhắn trong conversation là đã đọc.
     * Tự động đánh dấu khi user mở conversation.
     * 
     * @param conversationId ID của cuộc trò chuyện
     * @param userEmail Email của người đọc
     */
    void markConversationAsRead(UUID conversationId, String userEmail);

    /**
     * ⭐ Đánh dấu một tin nhắn cụ thể là đã đọc.
     * 
     * @param messageId ID của tin nhắn
     * @param userEmail Email của người đọc
     */
    void markMessageAsRead(UUID messageId, String userEmail);

    /**
     * ⭐ Đếm số tin nhắn chưa đọc trong một conversation.
     * 
     * @param conversationId ID của cuộc trò chuyện
     * @param userEmail Email của người dùng
     * @return Số lượng tin nhắn chưa đọc
     */
    long countUnreadMessages(UUID conversationId, String userEmail);
}
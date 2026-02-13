package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Message;

/**
 * Repository interface cho thực thể Message.
 * 
 * <p>Quản lý các tin nhắn trong hệ thống chat. Cung cấp các phương thức để
 * lưu trữ và truy vấn lịch sử tin nhắn của một cuộc trò chuyện.</p>
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Lấy lịch sử tin nhắn của một cuộc trò chuyện cụ thể, có phân trang.
     * <p>Phương thức này là cốt lõi của chức năng chat. Khi người dùng mở một
     * cuộc trò chuyện, client sẽ gọi API để tải về trang tin nhắn đầu tiên.
     * Khi người dùng cuộn lên trên, client sẽ tiếp tục gọi API để tải các
     * trang tin nhắn cũ hơn (infinite scrolling).</p>
     * <p>Kết quả được sắp xếp theo thời gian tạo mới nhất để hiển thị đúng
     * thứ tự của một cuộc trò chuyện.</p>
     *
     * @param conversationId ID của cuộc trò chuyện.
     * @param pageable       Thông tin phân trang.
     * @return một trang (Page) chứa danh sách các tin nhắn.
     */
    Page<Message> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    /**
     * ⭐ Đánh dấu tất cả tin nhắn trong conversation là đã đọc.
     * CHỈ đánh dấu những tin nhắn KHÔNG phải do user này gửi.
     * 
     * @param conversationId ID của cuộc trò chuyện
     * @param userId ID của người đọc
     * @return Số lượng tin nhắn đã được đánh dấu đã đọc
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.isRead = true 
        WHERE m.conversation.id = :conversationId 
        AND m.sender.id != :userId 
        AND m.isRead = false
        """)
    int markMessagesAsReadByConversation(
        @Param("conversationId") UUID conversationId, 
        @Param("userId") UUID userId
    );

    /**
     * ⭐ Đếm số tin nhắn chưa đọc trong một conversation.
     * CHỈ đếm những tin nhắn KHÔNG phải do user này gửi.
     * 
     * @param conversationId ID của cuộc trò chuyện
     * @param userId ID của người dùng
     * @return Số lượng tin nhắn chưa đọc
     */
    @Query("""
        SELECT COUNT(m) 
        FROM Message m 
        WHERE m.conversation.id = :conversationId 
        AND m.sender.id != :userId 
        AND m.isRead = false
        """)
    long countUnreadMessages(
        @Param("conversationId") UUID conversationId, 
        @Param("userId") UUID userId
    );

}
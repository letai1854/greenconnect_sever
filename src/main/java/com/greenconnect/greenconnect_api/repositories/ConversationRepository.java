package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Conversation;

/**
 * Repository interface cho thực thể Conversation.
 * 
 * <p>Quản lý các cuộc trò chuyện trong hệ thống chat hỗ trợ khách hàng.
 * Cung cấp các phương thức để tìm kiếm và truy vấn các cuộc trò chuyện.</p>
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID>, JpaSpecificationExecutor<Conversation> {

    // /**
    //  * Lấy danh sách các cuộc trò chuyện của một khách hàng cụ thể.
    //  *
    //  * @param customerId ID của khách hàng.
    //  * @param pageable   Thông tin phân trang.
    //  * @return một trang (Page) chứa danh sách các cuộc trò chuyện.
    //  */
    // Page<Conversation> findByCustomerIdOrderByUpdatedAtDesc(UUID customerId, Pageable pageable);

    /**
     * ⭐ BỔ SUNG: Lấy tất cả conversations với phân trang, sắp xếp theo updatedAt DESC
     * Dùng cho admin và support agent xem tất cả cuộc trò chuyện
     */
    Page<Conversation> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * ⭐ BỔ SUNG: Lấy conversations theo trạng thái với phân trang
     * Ví dụ: chỉ lấy những conversation có status = "NEW", "OPEN", etc.
     */
    Page<Conversation> findByStatusOrderByUpdatedAtDesc(String status, Pageable pageable);
    
    /**
     * ⭐ BỔ SUNG: Lấy conversations của một khách hàng cụ thể
     * Dùng khi khách hàng muốn xem lịch sử các cuộc trò chuyện của mình
     */
    Page<Conversation> findByCustomer_IdOrderByUpdatedAtDesc(UUID customerId, Pageable pageable);
    
    /**
     * ⭐ BỔ SUNG: Lấy conversations được assign cho một nhân viên cụ thể
     * Dùng khi nhân viên muốn xem những conversation được giao cho mình
     */
    Page<Conversation> findByAssignee_IdOrderByUpdatedAtDesc(UUID assigneeId, Pageable pageable);
    
    /**
     * ⭐ BỔ SUNG: Lấy conversation hiện tại (mới nhất) của một khách hàng theo email
     * Dùng khi cần lấy conversationId của user để chat
     */
    @Query("SELECT c FROM Conversation c WHERE c.customer.email = :email ORDER BY c.updatedAt DESC")
    Page<Conversation> findByCustomerEmailOrderByUpdatedAtDesc(@Param("email") String email, Pageable pageable);

    /**
     * ⭐ SEARCH: Tìm kiếm conversations theo tên hoặc email của customer
     * Dùng cho admin và customer support để tìm kiếm cuộc trò chuyện
     * @param searchTerm Từ khóa tìm kiếm (tên hoặc email của customer)
     * @param pageable Thông tin phân trang
     * @return Một trang chứa danh sách conversations phù hợp với từ khóa tìm kiếm
     */
    @Query("SELECT c FROM Conversation c WHERE " +
           "LOWER(c.customer.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.customer.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY c.updatedAt DESC")
    Page<Conversation> searchByCustomerNameOrEmail(@Param("searchTerm") String searchTerm, Pageable pageable);

    // /**
    //  * Tìm kiếm một cuộc trò chuyện liên quan đến một yêu cầu trả hàng cụ thể.
    //  *
    //  * @param returnRequestId ID của yêu cầu trả hàng.
    //  * @return một đối tượng {@link Optional} chứa {@link Conversation} nếu có.
    //  */
    // Optional<Conversation> findByReturnRequestId(UUID returnRequestId);

    // /**
    //  * Lấy danh sách các cuộc trò chuyện mà một nhân viên (admin/support) là thành viên.
    //  * <p>Sử dụng một câu lệnh JPQL tùy chỉnh để join với bảng trung gian
    //  * {@code conversation_participants}.</p>
    //  *
    //  * @param agentId  ID của nhân viên.
    //  * @param pageable Thông tin phân trang.
    //  * @return một trang (Page) chứa danh sách các cuộc trò chuyện mà nhân viên tham gia.
    //  */
    // @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :agentId ORDER BY c.updatedAt DESC")
    // Page<Conversation> findConversationsByParticipant(UUID agentId, Pageable pageable);
}
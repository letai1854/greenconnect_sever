package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Notification;
import com.greenconnect.greenconnect_api.enums.NotificationRecipient;

/**
 * Repository cho Notification
 * Chỉ chứa các query cơ bản cho Notification entity
 * Các query liên quan đến read status đã được chuyển sang NotificationRecipientRepository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Lấy tất cả thông báo của user (có phân trang)
     * Dùng cho CUSTOMER notifications
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Lấy tất cả thông báo MANAGER (có phân trang, sắp xếp theo ngày mới nhất)
     * Note: Không có filter isRead vì đã chuyển sang NotificationRecipientRepository
     */
    Page<Notification> findByRecipientOrderByCreatedAtDesc(NotificationRecipient recipient, Pageable pageable);
    
    /**
     * Lấy thông báo MANAGER theo khoảng thời gian (có phân trang)
     * @param recipient Recipient type (MANAGER)
     * @param startDate Ngày bắt đầu
     * @param pageable Phân trang
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.createdAt >= :startDate ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientAndCreatedAtAfter(
        @Param("recipient") NotificationRecipient recipient, 
        @Param("startDate") java.time.LocalDateTime startDate, 
        Pageable pageable
    );
    
    /**
     * Lấy tất cả thông báo CUSTOMER của user (có phân trang, sắp xếp theo ngày mới nhất)
     */
    Page<Notification> findByUserIdAndRecipientOrderByCreatedAtDesc(UUID userId, NotificationRecipient recipient, Pageable pageable);
    
    /**
     * Lấy thông báo CUSTOMER theo khoảng thời gian (có phân trang)
     * @param userId User ID
     * @param recipient Recipient type (CUSTOMER)
     * @param startDate Ngày bắt đầu
     * @param pageable Phân trang
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.recipient = :recipient AND n.createdAt >= :startDate ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndRecipientAndCreatedAtAfterOrderByCreatedAtDesc(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient, 
        @Param("startDate") java.time.LocalDateTime startDate, 
        Pageable pageable
    );
    
    /**
     * Lấy 6 thông báo CUSTOMER gần đây nhất
     * @param userId User ID
     * @param recipient Recipient type (CUSTOMER)
     */
    List<Notification> findTop6ByUserIdAndRecipientOrderByCreatedAtDesc(UUID userId, NotificationRecipient recipient);
}

package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.NotificationRecipientEntity;
import com.greenconnect.greenconnect_api.enums.NotificationRecipient;

/**
 * Repository cho NotificationRecipient
 * Quản lý người nhận và trạng thái đọc của từng thông báo
 */
@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipientEntity, UUID> {
    
    /**
     * Lấy tất cả thông báo của user (có phân trang)
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    /**
     * Lấy thông báo chưa đọc của user
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId AND nr.isRead = false " +
           "ORDER BY n.createdAt DESC")
    List<NotificationRecipientEntity> findByUserIdAndIsReadFalse(@Param("userId") UUID userId);
    
    /**
     * Đếm số thông báo chưa đọc
     */
    long countByUserIdAndIsReadFalse(UUID userId);
    
    /**
     * Đánh dấu một thông báo của user là đã đọc
     */
    @Modifying
    @Query("UPDATE NotificationRecipientEntity nr " +
           "SET nr.isRead = true, nr.readAt = CURRENT_TIMESTAMP " +
           "WHERE nr.notification.id = :notificationId AND nr.user.id = :userId AND nr.isRead = false")
    int markAsRead(@Param("notificationId") UUID notificationId, @Param("userId") UUID userId);
    
    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     */
    @Modifying
    @Query("UPDATE NotificationRecipientEntity nr " +
           "SET nr.isRead = true, nr.readAt = CURRENT_TIMESTAMP " +
           "WHERE nr.user.id = :userId AND nr.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);
    
    /**
     * Kiểm tra user đã đọc notification chưa
     */
    @Query("SELECT nr.isRead FROM NotificationRecipientEntity nr " +
           "WHERE nr.notification.id = :notificationId AND nr.user.id = :userId")
    Optional<Boolean> isReadByUser(@Param("notificationId") UUID notificationId, @Param("userId") UUID userId);
    
    // ========== QUERY METHODS CHO MANAGER ==========
    
    /**
     * Lấy tất cả thông báo MANAGER cho user cụ thể (có phân trang)
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId AND n.recipient = :recipient " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> findByUserIdAndRecipient(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        Pageable pageable
    );
    
    /**
     * Lấy thông báo MANAGER theo khoảng thời gian cho user cụ thể
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId AND n.recipient = :recipient AND n.createdAt >= :startDate " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> findByUserIdAndRecipientAndCreatedAtAfter(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        @Param("startDate") LocalDateTime startDate,
        Pageable pageable
    );
    
    /**
     * Lấy thông báo MANAGER theo trạng thái đọc cho user cụ thể
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId AND n.recipient = :recipient AND nr.isRead = :isRead " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> findByUserIdAndRecipientAndIsRead(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        @Param("isRead") boolean isRead,
        Pageable pageable
    );
    
    /**
     * Đếm số thông báo MANAGER chưa đọc của user
     */
    @Query("SELECT COUNT(nr) FROM NotificationRecipientEntity nr " +
           "JOIN nr.notification n " +
           "WHERE nr.user.id = :userId AND n.recipient = :recipient AND nr.isRead = false")
    long countByUserIdAndRecipientAndIsReadFalse(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient
    );
    
    /**
     * Tìm kiếm thông báo MANAGER theo từ khóa (trong title và message)
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId " +
           "AND n.recipient = :recipient " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> searchByUserIdAndRecipient(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        @Param("keyword") String keyword,
        Pageable pageable
    );
    
    /**
     * Tìm kiếm thông báo MANAGER theo từ khóa và khoảng thời gian
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId " +
           "AND n.recipient = :recipient " +
           "AND n.createdAt >= :startDate " +
           "AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationRecipientEntity> searchByUserIdAndRecipientAndCreatedAtAfter(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        @Param("keyword") String keyword,
        @Param("startDate") LocalDateTime startDate,
        Pageable pageable
    );
    
    /**
     * Lấy danh sách thông báo MANAGER theo trạng thái đọc với limit
     */
    @Query("SELECT nr FROM NotificationRecipientEntity nr " +
           "JOIN FETCH nr.notification n " +
           "WHERE nr.user.id = :userId " +
           "AND n.recipient = :recipient " +
           "AND nr.isRead = :isRead " +
           "ORDER BY n.createdAt DESC")
    List<NotificationRecipientEntity> findTopByUserIdAndRecipientAndIsRead(
        @Param("userId") UUID userId,
        @Param("recipient") NotificationRecipient recipient,
        @Param("isRead") boolean isRead,
        Pageable pageable
    );
}

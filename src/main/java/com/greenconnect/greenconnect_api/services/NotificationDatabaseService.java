package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.response.NotificationResponse;
import com.greenconnect.greenconnect_api.enums.NotificationRecipient;

/**
 * Service interface cho quản lý thông báo trong database
 */
public interface NotificationDatabaseService {
    
    /**
     * Tạo thông báo mới
     * 
     * @param userId User nhận thông báo
     * @param type Loại thông báo (order, refund, message, etc.)
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @param link Link liên quan (optional)
     * @param recipient Đối tượng nhận (CUSTOMER hoặc MANAGER)
     * @return Thông báo đã tạo
     */
    NotificationResponse createNotification(UUID userId, String type, String title, String message, String link, NotificationRecipient recipient);
    
    /**
     * Tạo thông báo và gửi đến nhiều user (dùng cho MANAGER)
     * 
     * @param userIds Danh sách user ID nhận thông báo
     * @param type Loại thông báo
     * @param title Tiêu đề
     * @param message Nội dung
     * @param link Link (optional)
     * @param recipient MANAGER hoặc CUSTOMER
     * @return Số lượng recipient đã tạo
     */
    int createNotificationForMultipleUsers(List<UUID> userIds, String type, String title, String message, String link, NotificationRecipient recipient);
    
    /**
     * Lấy danh sách thông báo của user (có phân trang)
     */
    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);
    
    /**
     * Lấy thông báo chưa đọc của user
     */
    List<NotificationResponse> getUnreadNotifications(UUID userId);
    
    /**
     * Đếm số thông báo chưa đọc
     */
    long countUnreadNotifications(UUID userId);
    
    /**
     * Đánh dấu một thông báo là đã đọc (của user cụ thể)
     * 
     * @param notificationId ID của thông báo
     * @param userId ID của user (để đảm bảo chỉ cập nhật notification của user này)
     */
    void markAsRead(UUID notificationId, UUID userId);
    
    /**
     * Đánh dấu nhiều thông báo là đã đọc (của user cụ thể)
     * 
     * @param notificationIds Danh sách ID thông báo cần đánh dấu đã đọc
     * @param userId ID của user (để đảm bảo chỉ cập nhật notifications của user này)
     * @return Số lượng thông báo đã được cập nhật
     */
    int markMultipleAsRead(List<UUID> notificationIds, UUID userId);
    
    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     */
    int markAllAsRead(UUID userId);
    
    /**
     * Xóa thông báo
     */
    void deleteNotification(UUID notificationId);
    
    // ========== METHODS CHO MANAGER ==========
    
    /**
     * Lấy tất cả thông báo MANAGER của user cụ thể (có phân trang)
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @param pageable Phân trang
     */
    Page<NotificationResponse> getAllManagerNotifications(UUID userId, NotificationRecipient recipient, Pageable pageable);
    
    /**
     * Lấy thông báo MANAGER theo khoảng thời gian của user cụ thể
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @param startDate Ngày bắt đầu
     * @param pageable Phân trang
     */
    Page<NotificationResponse> getManagerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, java.time.LocalDateTime startDate, Pageable pageable);
    
    /**
     * Lấy thông báo MANAGER theo trạng thái đọc của user cụ thể
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @param isRead Trạng thái đọc
     * @param pageable Phân trang
     */
    Page<NotificationResponse> getManagerNotifications(UUID userId, NotificationRecipient recipient, boolean isRead, Pageable pageable);
    
    /**
     * Tìm kiếm thông báo MANAGER theo từ khóa
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @param keyword Từ khóa tìm kiếm (trong title và message)
     * @param pageable Phân trang
     */
    Page<NotificationResponse> searchManagerNotifications(UUID userId, NotificationRecipient recipient, String keyword, Pageable pageable);
    
    /**
     * Tìm kiếm thông báo MANAGER theo từ khóa và khoảng thời gian
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @param keyword Từ khóa tìm kiếm
     * @param startDate Ngày bắt đầu
     * @param pageable Phân trang
     */
    Page<NotificationResponse> searchManagerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, String keyword, java.time.LocalDateTime startDate, Pageable pageable);
    
    /**
     * Lấy 6 thông báo MANAGER gần đây (ưu tiên chưa đọc)
     * Nếu chưa đọc < 6 thì lấy thêm đã đọc để đủ 6
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @return List chứa tối đa 6 thông báo
     */
    List<NotificationResponse> getRecentManagerNotifications(UUID userId, NotificationRecipient recipient);
    
    /**
     * Đếm tổng số thông báo MANAGER chưa đọc của user
     * @param userId ID của user (admin/manager)
     * @param recipient MANAGER
     * @return Tổng số thông báo chưa đọc
     */
    long countUnreadManagerNotifications(UUID userId, NotificationRecipient recipient);
    
    // ========== METHODS CHO CUSTOMER ==========
    
    /**
     * Lấy thông báo CUSTOMER của user (có phân trang)
     */
    Page<NotificationResponse> getCustomerNotifications(UUID userId, NotificationRecipient recipient, Pageable pageable);
    
    /**
     * Lấy thông báo CUSTOMER theo khoảng thời gian (có phân trang)
     * @param userId ID của customer
     * @param recipient CUSTOMER
     * @param startDate Ngày bắt đầu
     * @param pageable Phân trang
     */
    Page<NotificationResponse> getCustomerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, java.time.LocalDateTime startDate, Pageable pageable);
    
    /**
     * Lấy 6 thông báo CUSTOMER gần đây
     * Sắp xếp theo thời gian mới nhất
     * @param userId ID của customer
     * @param recipient CUSTOMER
     * @return List chứa tối đa 6 thông báo
     */
    List<NotificationResponse> getRecentCustomerNotifications(UUID userId, NotificationRecipient recipient);
    
    /**
     * Tạo thông báo cho CUSTOMER và gửi FCM đến TẤT CẢ thiết bị (WEB + MOBILE)
     * Dùng khi: Admin cập nhật trạng thái đơn hàng, phản hồi yêu cầu, etc.
     * 
     * @param customerId ID của customer nhận thông báo
     * @param type Loại thông báo (order_status, request_reply, etc.)
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @param link Link liên quan (optional)
     * @return NotificationResponse đã tạo
     */
    NotificationResponse createNotificationForCustomer(UUID customerId, String type, String title, String message, String link);
}

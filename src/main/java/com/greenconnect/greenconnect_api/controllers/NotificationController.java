package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.response.NotificationResponse;
import com.greenconnect.greenconnect_api.enums.NotificationRecipient;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.NotificationDatabaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller quản lý thông báo trong database
 * Phục vụ cho web dashboard và mobile app
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationDatabaseService notificationDatabaseService;
    
    /**
     * Lấy danh sách thông báo của user hiện tại (có phân trang)
     * 
     * GET /api/notifications/my-notifications?page=0&size=20
     */
    @GetMapping("/my-notifications")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("📋 User {} đang lấy danh sách thông báo - Page: {}, Size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationDatabaseService.getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy danh sách thông báo chưa đọc
     * 
     * GET /api/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("📬 User {} đang lấy thông báo chưa đọc", userId);
        
        List<NotificationResponse> notifications = notificationDatabaseService.getUnreadNotifications(userId);
        
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Đếm số thông báo chưa đọc
     * 
     * GET /api/notifications/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("📊 User {} đang đếm thông báo chưa đọc", userId);
        
        long count = notificationDatabaseService.countUnreadNotifications(userId);
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Đánh dấu một thông báo là đã đọc
     * 
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("✅ User {} đánh dấu notification {} là đã đọc", userId, id);
        
        notificationDatabaseService.markAsRead(id, userId);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Đánh dấu nhiều thông báo là đã đọc
     * 
     * <p><b>Mô tả:</b></p>
     * <ul>
     *   <li>Cập nhật trạng thái đã đọc cho nhiều thông báo cùng lúc</li>
     *   <li>Truyền vào danh sách UUID của các thông báo</li>
     *   <li>CHỈ cập nhật các notification thuộc về user hiện tại (bảo mật)</li>
     *   <li>Trả về số lượng thông báo đã được cập nhật thành công</li>
     * </ul>
     * 
     * <p><b>Request Body:</b></p>
     * <pre>
     * [
     *   "uuid-1",
     *   "uuid-2",
     *   "uuid-3"
     * ]
     * </pre>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>PUT /api/notifications/read-multiple</li>
     * </ul>
     * 
     * @param notificationIds Danh sách UUID của các thông báo cần đánh dấu đã đọc
     * @param authentication Thông tin xác thực của user hiện tại
     * @return ResponseEntity chứa số lượng thông báo đã cập nhật
     */
    @PutMapping("/read-multiple")
    public ResponseEntity<Integer> markMultipleAsRead(
            @org.springframework.web.bind.annotation.RequestBody List<UUID> notificationIds,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("✅ User {} đánh dấu {} thông báo là đã đọc", userId, notificationIds.size());
        
        int count = notificationDatabaseService.markMultipleAsRead(notificationIds, userId);
        
        log.info("✅ User {} đã cập nhật {} thông báo", userId, count);
        return ResponseEntity.ok(count);
    }
    
    /**
     * Cập nhật trạng thái đã đọc cho thông báo MANAGER
     * 
     * <p><b>Mô tả:</b></p>
     * <ul>
     *   <li>Dùng cho admin/manager đánh dấu thông báo là đã đọc</li>
     *   <li>CHỈ cập nhật record notification_recipients của manager hiện tại</li>
     *   <li>Các manager khác vẫn giữ nguyên trạng thái đọc của họ</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>PUT /api/notifications/manager/{id}/mark-read</li>
     * </ul>
     * 
     * @param id ID của thông báo cần cập nhật
     * @param authentication Thông tin xác thực của manager hiện tại
     * @return ResponseEntity với status OK
     */
    @PutMapping("/manager/{id}/mark-read")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<Void> markManagerNotificationAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("✅ Manager {} đánh dấu notification {} là đã đọc", userId, id);
        
        notificationDatabaseService.markAsRead(id, userId);
        
        log.info("✅ Đã cập nhật trạng thái đọc cho notification {} của manager {}", id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     * 
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("✅ User {} đánh dấu tất cả thông báo là đã đọc", userId);
        
        int count = notificationDatabaseService.markAllAsRead(userId);
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Xóa một thông báo
     * 
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        log.info("🗑️ Xóa notification {}", id);
        
        notificationDatabaseService.deleteNotification(id);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Helper method để lấy user ID từ Authentication
     */
    private UUID getCurrentUserId(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return principal.getUserId();
    }
    
    // ========== ENDPOINTS CHO MANAGER (ADMIN) ==========
    
    /**
     * Lấy danh sách thông báo cho MANAGER với phân trang và bộ lọc theo thời gian
     * 
     * <p><b>Bộ lọc:</b></p>
     * <ul>
     *   <li>all: Tất cả thông báo (mặc định)</li>
     *   <li>today: Thông báo hôm nay</li>
     *   <li>this_week: Thông báo trong tuần này</li>
     *   <li>this_month: Thông báo trong tháng này</li>
     *   <li>6_month: Thông báo trong 6 tháng gần đây</li>
     *   <li>1_year: Thông báo trong 1 năm gần đây</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /api/notifications/manager?page=0&size=20 - Tất cả thông báo</li>
     *   <li>GET /api/notifications/manager?filter=today&page=0&size=20 - Thông báo hôm nay</li>
     *   <li>GET /api/notifications/manager?filter=this_week&page=0&size=20 - Thông báo tuần này</li>
     * </ul>
     * 
     * @param filter Bộ lọc (all, today, this_week, this_month, 6_month, 1_year)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng thông báo mỗi trang (mặc định 20)
     * @return Page chứa danh sách thông báo MANAGER
     */
    @GetMapping("/manager")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<Page<NotificationResponse>> getManagerNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("📊 Manager {} lấy danh sách thông báo - Filter: {}, Page: {}, Size: {}", userId, filter, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications;
        
        java.time.LocalDateTime startDate;
        
        switch (filter.toLowerCase()) {
            case "today":
                startDate = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                notifications = notificationDatabaseService.getManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, startDate, pageable);
                break;
            case "this_week":
                startDate = java.time.LocalDateTime.now().minusWeeks(1);
                notifications = notificationDatabaseService.getManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, startDate, pageable);
                break;
            case "this_month":
                startDate = java.time.LocalDateTime.now().minusMonths(1);
                notifications = notificationDatabaseService.getManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, startDate, pageable);
                break;
            case "6_month":
                startDate = java.time.LocalDateTime.now().minusMonths(6);
                notifications = notificationDatabaseService.getManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, startDate, pageable);
                break;
            case "1_year":
                startDate = java.time.LocalDateTime.now().minusYears(1);
                notifications = notificationDatabaseService.getManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, startDate, pageable);
                break;
            default: // "all"
                notifications = notificationDatabaseService.getAllManagerNotifications(userId, NotificationRecipient.MANAGER, pageable);
                break;
        }
        
        log.info("✅ Tìm thấy {} thông báo MANAGER cho user {} (filter: {})", notifications.getTotalElements(), userId, filter);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy 6 thông báo MANAGER gần đây (ưu tiên chưa đọc)
     * 
     * <p><b>Mô tả:</b></p>
     * <ul>
     *   <li>Lấy tối đa 6 thông báo mới nhất</li>
     *   <li>Ưu tiên lấy thông báo chưa đọc trước</li>
     *   <li>Nếu chưa đọc < 6 thì lấy thêm đã đọc để đủ 6</li>
     *   <li>Sắp xếp theo thời gian mới nhất</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /notifications/manager/recent</li>
     * </ul>
     * 
     * @return Danh sách 6 thông báo gần đây
     */
    @GetMapping("/manager/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<List<NotificationResponse>> getRecentManagerNotifications(
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("📋 Manager {} lấy 6 thông báo gần đây", userId);
        
        List<NotificationResponse> notifications = notificationDatabaseService
                .getRecentManagerNotifications(userId, NotificationRecipient.MANAGER);
        
        log.info("✅ Trả về {} thông báo gần đây cho manager {}", 
                notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Tìm kiếm thông báo MANAGER theo từ khóa
     * 
     * <p><b>Mô tả:</b></p>
     * <ul>
     *   <li>Tìm kiếm trong title và message của thông báo</li>
     *   <li>Kết hợp với bộ lọc theo thời gian (tùy chọn)</li>
     *   <li>Tự động sắp xếp theo ngày mới nhất</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /api/notifications/manager/search?keyword=đơn hàng&page=0&size=20 - Tìm "đơn hàng"</li>
     *   <li>GET /api/notifications/manager/search?keyword=hoàn tiền&filter=today&page=0&size=20 - Tìm "hoàn tiền" hôm nay</li>
     *   <li>GET /api/notifications/manager/search?keyword=ORD123456&page=0&size=20 - Tìm theo mã đơn hàng</li>
     * </ul>
     * 
     * @param keyword Từ khóa tìm kiếm (tìm trong title và message)
     * @param filter Bộ lọc theo thời gian (all, today, this_week, this_month, 6_month, 1_year) - mặc định "all"
     * @param page Số trang (mặc định 0)
     * @param size Số lượng thông báo mỗi trang (mặc định 20)
     * @return Page chứa danh sách thông báo MANAGER khớp với từ khóa
     */
    @GetMapping("/manager/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<Page<NotificationResponse>> searchManagerNotifications(
            Authentication authentication,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("🔍 Manager {} tìm kiếm thông báo - Keyword: '{}', Filter: {}, Page: {}, Size: {}", 
                userId, keyword, filter, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications;
        
        java.time.LocalDateTime startDate;
        
        switch (filter.toLowerCase()) {
            case "today":
                startDate = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                notifications = notificationDatabaseService.searchManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, keyword, startDate, pageable);
                break;
            case "this_week":
                startDate = java.time.LocalDateTime.now().minusWeeks(1);
                notifications = notificationDatabaseService.searchManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, keyword, startDate, pageable);
                break;
            case "this_month":
                startDate = java.time.LocalDateTime.now().minusMonths(1);
                notifications = notificationDatabaseService.searchManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, keyword, startDate, pageable);
                break;
            case "6_month":
                startDate = java.time.LocalDateTime.now().minusMonths(6);
                notifications = notificationDatabaseService.searchManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, keyword, startDate, pageable);
                break;
            case "1_year":
                startDate = java.time.LocalDateTime.now().minusYears(1);
                notifications = notificationDatabaseService.searchManagerNotificationsByTimeRange(userId, NotificationRecipient.MANAGER, keyword, startDate, pageable);
                break;
            default: // "all"
                notifications = notificationDatabaseService.searchManagerNotifications(userId, NotificationRecipient.MANAGER, keyword, pageable);
                break;
        }
        
        log.info("✅ Tìm thấy {} thông báo MANAGER cho user {} (keyword: '{}', filter: {})", 
                notifications.getTotalElements(), userId, keyword, filter);
        return ResponseEntity.ok(notifications);
    }
    
    // ========== ENDPOINTS CHO CUSTOMER ==========
    
    /**
     * Lấy danh sách thông báo cho CUSTOMER với phân trang và bộ lọc theo thời gian
     * 
     * <p><b>Bộ lọc:</b></p>
     * <ul>
     *   <li>all: Tất cả thông báo (mặc định)</li>
     *   <li>today: Thông báo hôm nay</li>
     *   <li>this_week: Thông báo trong tuần này</li>
     *   <li>this_month: Thông báo trong tháng này</li>
     *   <li>6_month: Thông báo trong 6 tháng gần đây</li>
     *   <li>1_year: Thông báo trong 1 năm gần đây</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /api/notifications/customer?page=0&size=20 - Tất cả thông báo</li>
     *   <li>GET /api/notifications/customer?filter=today&page=0&size=20 - Thông báo hôm nay</li>
     *   <li>GET /api/notifications/customer?filter=this_week&page=0&size=20 - Thông báo tuần này</li>
     * </ul>
     * 
     * @param filter Bộ lọc (all, today, this_week, this_month, 6_month, 1_year)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng thông báo mỗi trang (mặc định 20)
     * @return Page chứa danh sách thông báo CUSTOMER
     */
    @GetMapping("/customer")
    public ResponseEntity<Page<NotificationResponse>> getCustomerNotificationsWithFilter(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("📊 Customer {} lấy danh sách thông báo - Page: {}, Size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationDatabaseService.getCustomerNotifications(userId, NotificationRecipient.CUSTOMER, pageable);
        
        log.info("✅ Tìm thấy {} thông báo CUSTOMER cho user {}", notifications.getTotalElements(), userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy 6 thông báo CUSTOMER gần đây
     * 
     * <p><b>Mô tả:</b></p>
     * <ul>
     *   <li>Lấy tối đa 6 thông báo mới nhất</li>
     *   <li>Sắp xếp theo thời gian mới nhất</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /notifications/customer/recent</li>
     * </ul>
     * 
     * @return Danh sách 6 thông báo gần đây
     */
    @GetMapping("/customer/recent")
    public ResponseEntity<List<NotificationResponse>> getRecentCustomerNotifications(
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("📋 Customer {} lấy 6 thông báo gần đây", userId);
        
        List<NotificationResponse> notifications = notificationDatabaseService
                .getRecentCustomerNotifications(userId, NotificationRecipient.CUSTOMER);
        
        log.info("✅ Trả về {} thông báo gần đây cho customer {}", 
                notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy danh sách thông báo cho CUSTOMER cụ thể với phân trang (endpoint cũ, dùng userId từ path)
     * 
     * <p><b>Đặc điểm:</b></p>
     * <ul>
     *   <li>Chỉ lấy thông báo của user cụ thể</li>
     *   <li>Chỉ lấy thông báo với recipient = CUSTOMER</li>
     *   <li>Tự động sắp xếp theo ngày mới nhất</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>GET /api/notifications/customer/{userId}?page=0&size=20</li>
     * </ul>
     * 
     * @param userId ID của user cần lấy thông báo
     * @param page Số trang (mặc định 0)
     * @param size Số lượng thông báo mỗi trang (mặc định 20)
     * @return Page chứa danh sách thông báo CUSTOMER
     */
    @GetMapping("/customer/{userId}")
    public ResponseEntity<Page<NotificationResponse>> getCustomerNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("📄 Lấy danh sách thông báo CUSTOMER cho user: {}, Page: {}, Size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationDatabaseService.getCustomerNotifications(userId, NotificationRecipient.CUSTOMER, pageable);
        
        log.info("✅ Tìm thấy {} thông báo CUSTOMER cho user {}", notifications.getTotalElements(), userId);
        return ResponseEntity.ok(notifications);
    }
}

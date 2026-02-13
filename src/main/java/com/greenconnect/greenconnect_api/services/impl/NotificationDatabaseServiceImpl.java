package com.greenconnect.greenconnect_api.services.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.response.NotificationResponse;
import com.greenconnect.greenconnect_api.entities.Notification;
import com.greenconnect.greenconnect_api.entities.NotificationRecipientEntity;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.NotificationRecipient;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.NotificationRecipientRepository;
import com.greenconnect.greenconnect_api.repositories.NotificationRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.NotificationDatabaseService;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.greenconnect.greenconnect_api.services.FcmPushService;
import com.greenconnect.greenconnect_api.enums.DeviceType;

/**
 * Implementation của NotificationDatabaseService
 * Quản lý thông báo trong database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDatabaseServiceImpl implements NotificationDatabaseService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;
    
    @Override
    @Transactional
    public NotificationResponse createNotification(UUID userId, String type, String title, String message, String link, NotificationRecipient recipient) {
        log.info("📢 Tạo thông báo mới - UserId: {}, Type: {}, Title: {}, Recipient: {}", userId, type, title, recipient);
        
        // Validate user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ Không tìm thấy user: {}", userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });
        
        // Tạo notification
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .recipient(recipient)
                .build();
        
        notification = notificationRepository.save(notification);
        
        log.info("✅ Tạo thông báo thành công - NotificationId: {}, Recipient: {}", notification.getId(), recipient);
        
        return mapToResponse(notification);
    }
    
    @Override
    @Transactional
    public int createNotificationForMultipleUsers(List<UUID> userIds, String type, String title, String message, String link, NotificationRecipient recipient) {
        log.info("📢 Tạo thông báo cho nhiều user - Count: {}, Type: {}, Title: {}, Recipient: {}", 
                userIds.size(), type, title, recipient);
        
        if (userIds == null || userIds.isEmpty()) {
            log.warn("⚠️ Danh sách userIds trống, không tạo thông báo");
            return 0;
        }
        
        // Lấy user đầu tiên làm "chủ sở hữu" của notification (user tạo đơn hàng)
        // Trong trường hợp này, userId đầu tiên là customer
        User owner = userRepository.findById(userIds.get(0))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Tạo 1 notification duy nhất
        Notification notification = Notification.builder()
                .user(owner)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .recipient(recipient)
                .build();
        
        notification = notificationRepository.save(notification);
        log.info("✅ Đã tạo notification: {}", notification.getId());
        
        // Lấy tất cả admin/manager IDs để gửi thông báo
        List<UUID> managerIds = userRepository.findUserIdsByRoles(
            Arrays.asList(Role.ADMIN, Role.CUSTOMER_SUPPORT)
        );
        
        log.info("📤 Gửi thông báo đến {} managers", managerIds.size());
        
        // Tạo NotificationRecipientEntity cho từng manager
        int createdCount = 0;
        for (UUID managerId : managerIds) {
            try {
                User manager = userRepository.findById(managerId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                
                NotificationRecipientEntity recipientEntity = NotificationRecipientEntity.builder()
                        .notification(notification)
                        .user(manager)
                        .isRead(false)
                        .build();
                
                notificationRecipientRepository.save(recipientEntity);
                createdCount++;
            } catch (Exception e) {
                log.error("❌ Lỗi tạo recipient cho manager {}: {}", managerId, e.getMessage());
            }
        }
        
        log.info("✅ Đã tạo thành công {}/{} notification recipients", createdCount, managerIds.size());
        
        // Gửi FCM push notification đến tất cả managers qua WEB
        if (createdCount > 0 && recipient == NotificationRecipient.MANAGER) {
            try {
                log.info("📤 [FCM] Gửi push notification đến managers...");
                int fcmSent = fcmPushService.sendToAllManagersWeb(title, message);
                log.info("✅ [FCM] Đã gửi thành công {} push notifications", fcmSent);
            } catch (Exception e) {
                // Không throw exception để không ảnh hưởng đến việc lưu notification
                log.error("❌ [FCM] Lỗi khi gửi push notification: {}", e.getMessage());
            }
        }
        
        return createdCount;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        log.info("📋 Lấy danh sách thông báo - UserId: {}, Page: {}, Size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        log.info("✅ Tìm thấy {} thông báo cho user {}", notifications.getTotalElements(), userId);
        
        return notifications.map(this::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        log.info("📬 Lấy thông báo chưa đọc - UserId: {}", userId);
        
        // Sử dụng NotificationRecipientRepository để lấy notification chưa đọc
        List<NotificationRecipientEntity> recipientEntities = notificationRecipientRepository
                .findByUserIdAndIsReadFalse(userId);
        
        log.info("✅ Tìm thấy {} thông báo chưa đọc cho user {}", recipientEntities.size(), userId);
        
        return recipientEntities.stream()
                .map(this::mapRecipientToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(UUID userId) {
        log.info("📊 Đếm thông báo chưa đọc - UserId: {}", userId);
        
        // Sử dụng NotificationRecipientRepository để đếm notification chưa đọc
        long count = notificationRecipientRepository.countByUserIdAndIsReadFalse(userId);
        
        log.info("✅ User {} có {} thông báo chưa đọc", userId, count);
        
        return count;
    }
    
    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        log.info("✅ Đánh dấu đã đọc - NotificationId: {}, UserId: {}", notificationId, userId);
        
        // Sử dụng NotificationRecipientRepository để cập nhật theo userId
        int updated = notificationRecipientRepository.markAsRead(notificationId, userId);
        
        if (updated == 0) {
            log.warn("⚠️ Không tìm thấy notification {} cho user {} hoặc đã đọc rồi", notificationId, userId);
        } else {
            log.info("✅ Đã đánh dấu notification {} là đã đọc cho user {}", notificationId, userId);
        }
    }
    
    @Override
    @Transactional
    public int markMultipleAsRead(List<UUID> notificationIds, UUID userId) {
        log.info("✅ Đánh dấu nhiều thông báo đã đọc - Count: {}, UserId: {}", notificationIds.size(), userId);
        
        if (notificationIds == null || notificationIds.isEmpty()) {
            log.warn("⚠️ Danh sách notificationIds trống");
            return 0;
        }
        
        int totalUpdated = 0;
        
        for (UUID notificationId : notificationIds) {
            try {
                // Chỉ cập nhật notification của user này
                int updated = notificationRecipientRepository.markAsRead(notificationId, userId);
                totalUpdated += updated;
            } catch (Exception e) {
                log.error("❌ Lỗi khi đánh dấu notification {} đã đọc cho user {}: {}", 
                         notificationId, userId, e.getMessage());
            }
        }
        
        log.info("✅ Đã đánh dấu {}/{} thông báo là đã đọc cho user {}", totalUpdated, notificationIds.size(), userId);
        
        return totalUpdated;
    }
    
    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        log.info("✅ Đánh dấu tất cả thông báo đã đọc - UserId: {}", userId);
        
        // Sử dụng NotificationRecipientRepository để cập nhật
        int updated = notificationRecipientRepository.markAllAsRead(userId);
        
        log.info("✅ Đã đánh dấu {} thông báo là đã đọc cho user {}", updated, userId);
        
        return updated;
    }
    
    @Override
    @Transactional
    public void deleteNotification(UUID notificationId) {
        log.info("🗑️ Xóa thông báo - NotificationId: {}", notificationId);
        
        if (!notificationRepository.existsById(notificationId)) {
            log.error("❌ Không tìm thấy notification: {}", notificationId);
        }
        
        notificationRepository.deleteById(notificationId);
        
        log.info("✅ Đã xóa notification {}", notificationId);
    }
    
    // ========== IMPLEMENTATIONS CHO MANAGER ==========
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllManagerNotifications(UUID userId, NotificationRecipient recipient, Pageable pageable) {
        log.info("📋 Lấy tất cả thông báo MANAGER - UserId: {}, Page: {}, Size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> recipients = notificationRecipientRepository
                .findByUserIdAndRecipient(userId, recipient, pageable);
        
        log.info("✅ Tìm thấy {} thông báo MANAGER cho user {}", recipients.getTotalElements(), userId);
        
        return recipients.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getManagerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, java.time.LocalDateTime startDate, Pageable pageable) {
        log.info("📋 Lấy thông báo MANAGER từ ngày: {} - UserId: {}, Page: {}, Size: {}", 
                startDate, userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> recipients = notificationRecipientRepository
                .findByUserIdAndRecipientAndCreatedAtAfter(userId, recipient, startDate, pageable);
        
        log.info("✅ Tìm thấy {} thông báo MANAGER trong khoảng thời gian cho user {}", recipients.getTotalElements(), userId);
        
        return recipients.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getManagerNotifications(UUID userId, NotificationRecipient recipient, boolean isRead, Pageable pageable) {
        log.info("📋 Lấy thông báo MANAGER - UserId: {}, IsRead: {}, Page: {}, Size: {}", 
                userId, isRead, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> recipients = notificationRecipientRepository
                .findByUserIdAndRecipientAndIsRead(userId, recipient, isRead, pageable);
        
        log.info("✅ Tìm thấy {} thông báo MANAGER (isRead={}) cho user {}", recipients.getTotalElements(), isRead, userId);
        
        return recipients.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchManagerNotifications(UUID userId, NotificationRecipient recipient, String keyword, Pageable pageable) {
        log.info("🔍 Tìm kiếm thông báo MANAGER - UserId: {}, Keyword: '{}', Page: {}, Size: {}", 
                userId, keyword, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> recipients = notificationRecipientRepository
                .searchByUserIdAndRecipient(userId, recipient, keyword, pageable);
        
        log.info("✅ Tìm thấy {} thông báo MANAGER khớp với keyword '{}' cho user {}", 
                recipients.getTotalElements(), keyword, userId);
        
        return recipients.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchManagerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, String keyword, java.time.LocalDateTime startDate, Pageable pageable) {
        log.info("🔍 Tìm kiếm thông báo MANAGER theo thời gian - UserId: {}, Keyword: '{}', StartDate: {}, Page: {}, Size: {}", 
                userId, keyword, startDate, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> recipients = notificationRecipientRepository
                .searchByUserIdAndRecipientAndCreatedAtAfter(userId, recipient, keyword, startDate, pageable);
        
        log.info("✅ Tìm thấy {} thông báo MANAGER khớp với keyword '{}' từ {} cho user {}", 
                recipients.getTotalElements(), keyword, startDate, userId);
        
        return recipients.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentManagerNotifications(UUID userId, NotificationRecipient recipient) {
        log.info("📋 Lấy 6 thông báo MANAGER gần đây - UserId: {}", userId);
        
        // Lấy tối đa 6 thông báo chưa đọc
        List<NotificationRecipientEntity> unreadNotifications = notificationRecipientRepository
                .findTopByUserIdAndRecipientAndIsRead(userId, recipient, false, PageRequest.of(0, 6));
        
        log.info("📬 Tìm thấy {} thông báo chưa đọc", unreadNotifications.size());
        
        // Nếu chưa đủ 6, lấy thêm thông báo đã đọc
        if (unreadNotifications.size() < 6) {
            int remaining = 6 - unreadNotifications.size();
            log.info("📖 Lấy thêm {} thông báo đã đọc để đủ 6", remaining);
            
            List<NotificationRecipientEntity> readNotifications = notificationRecipientRepository
                    .findTopByUserIdAndRecipientAndIsRead(userId, recipient, true, PageRequest.of(0, remaining));
            
            // Kết hợp 2 danh sách: chưa đọc trước, đã đọc sau
            List<NotificationRecipientEntity> allNotifications = new java.util.ArrayList<>(unreadNotifications);
            allNotifications.addAll(readNotifications);
            
            log.info("✅ Tổng cộng: {} chưa đọc + {} đã đọc = {} thông báo", 
                    unreadNotifications.size(), readNotifications.size(), allNotifications.size());
            
            return allNotifications.stream()
                    .map(this::mapRecipientToResponse)
                    .collect(Collectors.toList());
        }
        
        log.info("✅ Trả về {} thông báo chưa đọc", unreadNotifications.size());
        
        return unreadNotifications.stream()
                .map(this::mapRecipientToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countUnreadManagerNotifications(UUID userId, NotificationRecipient recipient) {
        log.info("📊 Đếm thông báo MANAGER chưa đọc - UserId: {}", userId);
        
        long count = notificationRecipientRepository.countByUserIdAndRecipientAndIsReadFalse(userId, recipient);
        
        log.info("✅ Tìm thấy {} thông báo chưa đọc", count);
        
        return count;
    }
    
    // ========== IMPLEMENTATIONS CHO CUSTOMER ==========
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getCustomerNotifications(UUID userId, NotificationRecipient recipient, Pageable pageable) {
        log.info("📋 Lấy thông báo CUSTOMER - UserId: {}, Page: {}, Size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> notifications = notificationRecipientRepository.findByUserIdAndRecipient(userId, recipient, pageable);
        
        log.info("✅ Tìm thấy {} thông báo CUSTOMER cho user {}", notifications.getTotalElements(), userId);
        
        return notifications.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getCustomerNotificationsByTimeRange(UUID userId, NotificationRecipient recipient, java.time.LocalDateTime startDate, Pageable pageable) {
        log.info("📋 Lấy thông báo CUSTOMER từ ngày: {} - UserId: {}, Page: {}, Size: {}", 
                startDate, userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<NotificationRecipientEntity> notifications = notificationRecipientRepository.findByUserIdAndRecipientAndCreatedAtAfter(userId, recipient, startDate, pageable);
        
        log.info("✅ Tìm thấy {} thông báo CUSTOMER trong khoảng thời gian cho user {}", notifications.getTotalElements(), userId);
        
        return notifications.map(this::mapRecipientToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentCustomerNotifications(UUID userId, NotificationRecipient recipient) {
        log.info("📋 Lấy 6 thông báo CUSTOMER gần đây - UserId: {}", userId);
        
        // Lấy tối đa 6 thông báo mới nhất từ notification_recipients để có isRead
        Page<NotificationRecipientEntity> recentPage = notificationRecipientRepository
                .findByUserIdAndRecipient(userId, recipient, PageRequest.of(0, 6));
        
        List<NotificationRecipientEntity> recentNotifications = recentPage.getContent();
        
        log.info("✅ Tìm thấy {} thông báo CUSTOMER gần đây cho user {}", recentNotifications.size(), userId);
        
        return recentNotifications.stream()
                .map(this::mapRecipientToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public NotificationResponse createNotificationForCustomer(UUID customerId, String type, String title, String message, String link) {
        log.info("📢 [CUSTOMER NOTIFICATION] ===== BẮT ĐẦU TẠO THÔNG BÁO CHO CUSTOMER =====");
        log.info("📢 [CUSTOMER NOTIFICATION] CustomerId: {}", customerId);
        log.info("📢 [CUSTOMER NOTIFICATION] Type: {}, Title: {}", type, title);
        
        // Validate customer tồn tại
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("❌ Không tìm thấy customer: {}", customerId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });
        
        // Tạo notification với recipient = CUSTOMER
        Notification notification = Notification.builder()
                .user(customer)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .recipient(NotificationRecipient.CUSTOMER)
                .build();
        
        notification = notificationRepository.save(notification);
        log.info("✅ [CUSTOMER NOTIFICATION] Đã lưu notification: {}", notification.getId());
        
        // Tạo NotificationRecipientEntity để track read status
        NotificationRecipientEntity recipientEntity = NotificationRecipientEntity.builder()
                .notification(notification)
                .user(customer)
                .isRead(false)
                .build();
        notificationRecipientRepository.save(recipientEntity);
        log.info("✅ [CUSTOMER NOTIFICATION] Đã tạo recipient record cho customer");
        
        // Gửi FCM push notification đến TẤT CẢ thiết bị của customer (WEB + MOBILE)
        try {
            log.info("📤 [CUSTOMER NOTIFICATION] Gửi FCM đến tất cả devices của customer...");
            int fcmSent = fcmPushService.sendToCustomerAllDevices(customerId, title, message);
            log.info("✅ [CUSTOMER NOTIFICATION] Đã gửi {} FCM notifications", fcmSent);
        } catch (Exception e) {
            // Không throw exception để không ảnh hưởng đến việc lưu notification
            log.error("❌ [CUSTOMER NOTIFICATION] Lỗi gửi FCM: {}", e.getMessage());
        }
        
        log.info("📢 [CUSTOMER NOTIFICATION] ===== KẾT THÚC =====");
        
        return mapToResponse(notification);
    }
    
    /**
     * Map Notification entity sang NotificationResponse DTO
     * Dùng cho CUSTOMER notifications - không có isRead vì không dùng NotificationRecipientEntity
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .userCode(notification.getUser().getUserCode())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .recipient(notification.getRecipient())
                .isRead(null) // CUSTOMER notifications không track read status
                .createdAt(notification.getCreatedAt())
                .build();
    }
    
    /**
     * Map NotificationRecipientEntity sang NotificationResponse DTO
     * Dùng cho manager endpoints - isRead là của user cụ thể
     */
    private NotificationResponse mapRecipientToResponse(NotificationRecipientEntity recipientEntity) {
        Notification notification = recipientEntity.getNotification();
        
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId()) // User tạo đơn hàng (customer)
                .userCode(notification.getUser().getUserCode())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .recipient(notification.getRecipient())
                .isRead(recipientEntity.getIsRead()) // Read status của manager đang xem
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

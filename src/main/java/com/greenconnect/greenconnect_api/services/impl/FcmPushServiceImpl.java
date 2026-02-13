package com.greenconnect.greenconnect_api.services.impl;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.greenconnect.greenconnect_api.entities.FcmToken;
import com.greenconnect.greenconnect_api.enums.DeviceType;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.repositories.FcmTokenRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.FcmPushService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation của FcmPushService
 * Gửi push notification qua Firebase Cloud Messaging
 */
@Service
@Slf4j
public class FcmPushServiceImpl implements FcmPushService {
    
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final FirebaseApp firebaseApp; // Có thể null nếu Firebase không được khởi tạo
    
    // Token hợp lệ trong 30 ngày (FCM token thường expired sau 2 tháng)
    private static final int TOKEN_VALID_DAYS = 30;
    
    // Constructor injection với optional FirebaseApp
    @Autowired
    public FcmPushServiceImpl(FcmTokenRepository fcmTokenRepository, 
                              UserRepository userRepository,
                              @Autowired(required = false) FirebaseApp firebaseApp) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userRepository = userRepository;
        this.firebaseApp = firebaseApp;
        
        if (firebaseApp != null) {
            log.info("✅ [FCM] FcmPushService đã khởi tạo với FirebaseApp");
        } else {
            log.warn("⚠️ [FCM] FcmPushService khởi tạo KHÔNG có FirebaseApp - Push notifications sẽ bị disable");
        }
    }
    
    @Override
    public boolean isFirebaseInitialized() {
        return firebaseApp != null && !FirebaseApp.getApps().isEmpty();
    }
    
    @Override
    public int sendToUser(UUID userId, String title, String message, DeviceType deviceType) {
        log.info("📤 [FCM] Gửi thông báo đến user: {} - Title: {}", userId, title);
        
        if (!isFirebaseInitialized()) {
            log.warn("⚠️ [FCM] Firebase chưa được khởi tạo, bỏ qua push notification");
            return 0;
        }
        
        // Lấy FCM tokens của user
        List<FcmToken> tokens = getValidTokens(userId, deviceType);
        
        if (tokens.isEmpty()) {
            log.info("ℹ️ [FCM] User {} không có FCM token {} hợp lệ", userId, deviceType);
            return 0;
        }
        
        return sendToTokens(tokens.stream().map(FcmToken::getToken).collect(Collectors.toList()), 
                           title, message, deviceType == DeviceType.WEB);
    }
    
    @Override
    public int sendToMultipleUsers(List<UUID> userIds, String title, String message, DeviceType deviceType) {
        log.info("📤 [FCM] Gửi thông báo đến {} users - Title: {}", userIds.size(), title);
        
        if (!isFirebaseInitialized()) {
            log.warn("⚠️ [FCM] Firebase chưa được khởi tạo, bỏ qua push notification");
            return 0;
        }
        
        // Thu thập tất cả tokens
        List<String> allTokens = new ArrayList<>();
        for (UUID userId : userIds) {
            List<FcmToken> tokens = getValidTokens(userId, deviceType);
            allTokens.addAll(tokens.stream().map(FcmToken::getToken).collect(Collectors.toList()));
        }
        
        if (allTokens.isEmpty()) {
            log.info("ℹ️ [FCM] Không có FCM token {} hợp lệ cho {} users", deviceType, userIds.size());
            return 0;
        }
        
        return sendToTokens(allTokens, title, message, deviceType == DeviceType.WEB);
    }
    
    @Override
    public int sendToAllManagersWeb(String title, String message) {
        log.info("📤 [FCM] ===== BẮT ĐẦU GỬI THÔNG BÁO WEB =====");
        log.info("📤 [FCM] Title: {}", title);
        log.info("📤 [FCM] Message: {}", message);
        
        // Debug Firebase status
        log.info("🔥 [FCM] firebaseApp is null? {}", firebaseApp == null);
        log.info("🔥 [FCM] FirebaseApp.getApps().isEmpty()? {}", FirebaseApp.getApps().isEmpty());
        
        if (!isFirebaseInitialized()) {
            log.warn("⚠️ [FCM] Firebase chưa được khởi tạo, bỏ qua push notification");
            return 0;
        }
        
        log.info("✅ [FCM] Firebase đã được khởi tạo, tiếp tục...");
        
        // Lấy tất cả user IDs của ADMIN và CUSTOMER_SUPPORT
        List<UUID> managerIds = userRepository.findUserIdsByRoles(
            Arrays.asList(Role.ADMIN, Role.CUSTOMER_SUPPORT)
        );
        
        log.info("👥 [FCM] Số lượng managers tìm thấy: {}", managerIds.size());
        
        if (managerIds.isEmpty()) {
            log.info("ℹ️ [FCM] Không tìm thấy managers nào");
            return 0;
        }
        
        // Debug: Log từng manager ID
        for (UUID managerId : managerIds) {
            log.info("👤 [FCM] Manager ID: {}", managerId);
        }
        
        // Thu thập tất cả WEB tokens của managers
        List<String> webTokens = new ArrayList<>();
        for (UUID managerId : managerIds) {
            // Debug: Log tất cả tokens của manager (không filter)
            List<FcmToken> allTokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(managerId);
            log.info("🔍 [FCM] Manager {} có {} active tokens tổng cộng", managerId, allTokens.size());
            
            for (FcmToken t : allTokens) {
                log.info("   📱 Token: deviceType={}, lastUpdated={}", 
                    t.getDeviceType(), 
                    t.getLastUpdated());
                log.info("   📱 FULL TOKEN: {}", t.getToken()); // Log đầy đủ token để so sánh
            }
            
            List<FcmToken> tokens = getValidTokens(managerId, DeviceType.WEB);
            log.info("🌐 [FCM] Manager {} có {} WEB tokens hợp lệ", managerId, tokens.size());
            webTokens.addAll(tokens.stream().map(FcmToken::getToken).collect(Collectors.toList()));
        }
        
        log.info("📊 [FCM] Tổng số WEB tokens thu thập được: {}", webTokens.size());
        
        if (webTokens.isEmpty()) {
            log.warn("⚠️ [FCM] Không có managers nào có FCM token WEB hợp lệ!");
            log.warn("⚠️ [FCM] Hãy kiểm tra: 1) Managers đã đăng nhập web chưa? 2) FCM token đã được lưu chưa?");
            return 0;
        }
        
        log.info("🔔 [FCM] Bắt đầu gửi đến {} WEB tokens", webTokens.size());
        
        int result = sendToTokens(webTokens, title, message, true);
        log.info("📤 [FCM] ===== KẾT THÚC GỬI THÔNG BÁO: {} thành công =====", result);
        return result;
    }
    
    @Override
    public int sendToCustomerAllDevices(UUID userId, String title, String message) {
        log.info("📤 [FCM] ===== GỬI THÔNG BÁO ĐẾN CUSTOMER {} TRÊN TẤT CẢ DEVICES =====", userId);
        log.info("📤 [FCM] Title: {}", title);
        log.info("📤 [FCM] Message: {}", message);
        
        if (!isFirebaseInitialized()) {
            log.warn("⚠️ [FCM] Firebase chưa được khởi tạo, bỏ qua push notification");
            return 0;
        }
        
        // Lấy TẤT CẢ active tokens của user (WEB + ANDROID + IOS)
        List<FcmToken> allTokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        log.info("📱 [FCM] Customer {} có {} active tokens tổng cộng", userId, allTokens.size());
        
        if (allTokens.isEmpty()) {
            log.info("ℹ️ [FCM] Customer {} không có FCM token nào", userId);
            return 0;
        }
        
        // Phân loại tokens theo device type
        List<String> webTokens = new ArrayList<>();
        List<String> mobileTokens = new ArrayList<>(); // Android + iOS
        
        LocalDateTime validAfter = getNowVietnam().minusDays(TOKEN_VALID_DAYS);
        
        for (FcmToken token : allTokens) {
            // Kiểm tra token còn hợp lệ
            if (token.getLastUpdated() != null && token.getLastUpdated().isBefore(validAfter)) {
                log.debug("⚠️ [FCM] Token đã cũ, bỏ qua: deviceType={}", token.getDeviceType());
                continue;
            }
            
            if (token.getDeviceType() == DeviceType.WEB) {
                webTokens.add(token.getToken());
                log.info("   🌐 WEB Token: {}...{}", 
                    token.getToken().substring(0, Math.min(20, token.getToken().length())),
                    token.getToken().substring(Math.max(0, token.getToken().length() - 10)));
            } else {
                // ANDROID hoặc IOS
                mobileTokens.add(token.getToken());
                log.info("   📱 {} Token: {}...{}", 
                    token.getDeviceType(),
                    token.getToken().substring(0, Math.min(20, token.getToken().length())),
                    token.getToken().substring(Math.max(0, token.getToken().length() - 10)));
            }
        }
        
        int totalSent = 0;
        
        // Gửi WEB notifications
        if (!webTokens.isEmpty()) {
            log.info("🌐 [FCM] Gửi đến {} WEB tokens", webTokens.size());
            totalSent += sendToTokens(webTokens, title, message, true);
        }
        
        // Gửi Mobile notifications (Android/iOS)
        if (!mobileTokens.isEmpty()) {
            log.info("📱 [FCM] Gửi đến {} MOBILE tokens", mobileTokens.size());
            totalSent += sendToTokens(mobileTokens, title, message, false);
        }
        
        log.info("📤 [FCM] ===== KẾT THÚC: {} thông báo gửi thành công =====", totalSent);
        return totalSent;
    }
    
    /**
     * Lấy danh sách FCM tokens hợp lệ của user
     * - Token phải active
     * - Token phải được cập nhật trong vòng TOKEN_VALID_DAYS ngày
     * - Filter theo deviceType nếu có
     */
    private List<FcmToken> getValidTokens(UUID userId, DeviceType deviceType) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(userId);
        
        LocalDateTime validAfter = getNowVietnam().minusDays(TOKEN_VALID_DAYS);
        
        return tokens.stream()
                .filter(token -> {
                    // Filter theo deviceType nếu có
                    if (deviceType != null && token.getDeviceType() != deviceType) {
                        return false;
                    }
                    // Kiểm tra token còn hợp lệ (được cập nhật gần đây)
                    if (token.getLastUpdated() != null && token.getLastUpdated().isBefore(validAfter)) {
                        log.debug("⚠️ [FCM] Token của user {} đã cũ, lastUpdated: {}", userId, token.getLastUpdated());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Gửi thông báo đến danh sách tokens
     * Sử dụng Multicast nếu có nhiều tokens (tối ưu hiệu năng)
     */
    private int sendToTokens(List<String> tokens, String title, String message, boolean isWeb) {
        if (tokens.isEmpty()) {
            return 0;
        }
        
        try {
            // Xóa duplicate tokens
            List<String> uniqueTokens = tokens.stream().distinct().collect(Collectors.toList());
            
            log.info("📨 [FCM] Bắt đầu gửi đến {} tokens (unique)", uniqueTokens.size());
            
            if (uniqueTokens.size() == 1) {
                // Gửi single message
                return sendSingleMessage(uniqueTokens.get(0), title, message, isWeb) ? 1 : 0;
            } else {
                // Gửi multicast (tối đa 500 tokens/batch)
                return sendMulticastMessage(uniqueTokens, title, message, isWeb);
            }
            
        } catch (Exception e) {
            log.error("❌ [FCM] Lỗi không mong muốn khi gửi notification: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Gửi thông báo đến một token
     */
    private boolean sendSingleMessage(String token, String title, String body, boolean isWeb) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    // Notification payload - để FCM tự hiển thị
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    // Data payload - để app xử lý
                    .putData("title", title)
                    .putData("body", body)
                    .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    .putData("type", "manager_notification");
            
            // Thêm AndroidConfig để đánh thức máy khi app tắt (terminated)
            if (!isWeb) {
                messageBuilder.setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH) // QUAN TRỌNG: Để đánh thức máy khi app tắt
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("high_importance_channel") // Phải khớp với Flutter
                                .setSound("default")
                                .setDefaultSound(true)
                                .setPriority(AndroidNotification.Priority.HIGH)
                                .build())
                        .build());
            }
            
            // Thêm WebpushConfig cho web notifications
            if (isWeb) {
                messageBuilder.setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .setIcon("/icons/Icon-192.png")
                                .setBadge("/icons/Icon-192.png")
                                .build())
                        // FCM options cho web
                        .setFcmOptions(com.google.firebase.messaging.WebpushFcmOptions.builder()
                                .setLink("/notifications") // Link khi click notification
                                .build())
                        .build());
            }
            
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("✅ [FCM] Gửi thành công - MessageId: {}", response);
            return true;
            
        } catch (FirebaseMessagingException e) {
            log.error("❌ [FCM] Lỗi gửi single message: {} - Code: {}", e.getMessage(), e.getMessagingErrorCode());
            handleTokenError(token, e);
            return false;
        }
    }
    
    /**
     * Gửi thông báo multicast đến nhiều tokens
     * Firebase giới hạn tối đa 500 tokens mỗi batch
     */
    private int sendMulticastMessage(List<String> tokens, String title, String body, boolean isWeb) {
        int successCount = 0;
        int batchSize = 500;
        
        // Chia thành các batch 500 tokens
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            
            try {
                MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                        .addAllTokens(batch)
                        // Notification payload - để FCM tự hiển thị
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        // Data payload - để app xử lý
                        .putData("title", title)
                        .putData("body", body)
                        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                        .putData("type", "manager_notification");
                
                // Thêm AndroidConfig để đánh thức máy khi app tắt (terminated)
                if (!isWeb) {
                    messageBuilder.setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH) // QUAN TRỌNG: Để đánh thức máy khi app tắt
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("high_importance_channel") // Phải khớp với Flutter
                                    .setSound("default")
                                    .setDefaultSound(true)
                                    .setPriority(AndroidNotification.Priority.HIGH)
                                    .build())
                            .build());
                }
                
                // Thêm WebpushConfig cho web notifications
                if (isWeb) {
                    messageBuilder.setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setIcon("/icons/Icon-192.png")
                                    .setBadge("/icons/Icon-192.png")
                                    .build())
                            // FCM options cho web
                            .setFcmOptions(com.google.firebase.messaging.WebpushFcmOptions.builder()
                                    .setLink("/notifications")
                                    .build())
                            .build());
                }
                
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(messageBuilder.build());
                
                int batchSuccess = response.getSuccessCount();
                successCount += batchSuccess;
                
                log.info("📊 [FCM] Batch {}/{}: {} success, {} failed", 
                        (i / batchSize) + 1, 
                        (int) Math.ceil(tokens.size() / (double) batchSize),
                        batchSuccess, 
                        response.getFailureCount());
                
                // Xử lý các token lỗi
                if (response.getFailureCount() > 0) {
                    handleBatchErrors(batch, response.getResponses());
                }
                
            } catch (FirebaseMessagingException e) {
                log.error("❌ [FCM] Lỗi gửi multicast batch: {}", e.getMessage());
            }
        }
        
        log.info("✅ [FCM] Tổng kết: {}/{} thông báo gửi thành công", successCount, tokens.size());
        return successCount;
    }
    
    /**
     * Xử lý lỗi token - XÓA token không hợp lệ khỏi database (dọn rác)
     * Với mô hình đa thiết bị, cần xóa hẳn token lỗi để tránh tích tụ rác
     */
    private void handleTokenError(String token, FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "UNKNOWN";
        
        // Nếu token không hợp lệ hoặc unregistered → XÓA HẲN khỏi database
        if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
            log.warn("🧹 [FCM] Token không hợp lệ ({}), XÓA khỏi DB: {}...", 
                    errorCode,
                    token.substring(0, Math.min(20, token.length())));
            
            // Xóa hẳn token thay vì deactivate (tránh rác tích tụ)
            fcmTokenRepository.findByToken(token).ifPresent(fcmToken -> {
                fcmTokenRepository.delete(fcmToken);
                log.info("🗑️ [FCM] Đã XÓA token lỗi của user: {}", fcmToken.getUser().getEmail());
            });
        }
    }
    
    /**
     * Xử lý lỗi batch - deactivate các token không hợp lệ
     */
    private void handleBatchErrors(List<String> tokens, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (!response.isSuccessful() && response.getException() != null) {
                handleTokenError(tokens.get(i), response.getException());
            }
        }
    }
    
    /**
     * ⏰ Helper: Lấy LocalDateTime hiện tại với timezone Việt Nam (Asia/Ho_Chi_Minh)
     * 
     * Không cần convert ZonedDateTime nữa vì đã set TimeZone.setDefault() trong @PostConstruct
     * của GreenconnectApiApplication.java, nên LocalDateTime.now() tự động là giờ Việt Nam.
     */
    private LocalDateTime getNowVietnam() {
        return LocalDateTime.now();
    }
}

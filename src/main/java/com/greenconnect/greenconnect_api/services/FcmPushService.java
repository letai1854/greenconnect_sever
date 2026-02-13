package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.DeviceType;

/**
 * Service interface cho Firebase Cloud Messaging (FCM) Push Notification
 * Gửi thông báo đến các thiết bị đã đăng ký FCM token
 */
public interface FcmPushService {
    
    /**
     * Gửi thông báo đến một user
     * 
     * @param userId ID của user nhận thông báo
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @param deviceType Loại thiết bị (WEB, ANDROID, IOS) - null = tất cả
     * @return Số thông báo gửi thành công
     */
    int sendToUser(UUID userId, String title, String message, DeviceType deviceType);
    
    /**
     * Gửi thông báo đến nhiều user
     * 
     * @param userIds Danh sách ID của các user nhận thông báo
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @param deviceType Loại thiết bị (WEB, ANDROID, IOS) - null = tất cả
     * @return Số thông báo gửi thành công
     */
    int sendToMultipleUsers(List<UUID> userIds, String title, String message, DeviceType deviceType);
    
    /**
     * Gửi thông báo đến tất cả ADMIN và CUSTOMER_SUPPORT qua WEB
     * 
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @return Số thông báo gửi thành công
     */
    int sendToAllManagersWeb(String title, String message);
    
    /**
     * Gửi thông báo đến một CUSTOMER trên TẤT CẢ thiết bị (WEB + ANDROID + IOS)
     * Dùng cho việc gửi thông báo cập nhật đơn hàng, phản hồi yêu cầu, etc.
     * 
     * @param userId ID của customer nhận thông báo
     * @param title Tiêu đề thông báo
     * @param message Nội dung thông báo
     * @return Số thông báo gửi thành công (trên tất cả devices)
     */
    int sendToCustomerAllDevices(UUID userId, String title, String message);
    
    /**
     * Kiểm tra Firebase có được khởi tạo thành công hay không
     * 
     * @return true nếu Firebase đã sẵn sàng
     */
    boolean isFirebaseInitialized();
}

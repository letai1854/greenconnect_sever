package com.greenconnect.greenconnect_api.enums;

/**
 * Trạng thái gửi tin nhắn
 * SENT: Tin nhắn đã được server lưu thành công vào DB
 * FAILED: Tin nhắn gửi thất bại (lỗi server, validation...)
 * 
 * Note: PENDING chỉ tồn tại trên client-side, không lưu vào DB
 */
public enum MessageStatus {
    /**
     * Tin nhắn đã được server lưu thành công vào DB
     */
    SENT,
    
    /**
     * Tin nhắn gửi thất bại (lỗi server, validation...)
     */
    FAILED
}

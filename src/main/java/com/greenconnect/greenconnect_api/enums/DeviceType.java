package com.greenconnect.greenconnect_api.enums;

/**
 * Loại thiết bị gửi push notification.
 */
public enum DeviceType {
    ANDROID("Android"),
    IOS("iOS"),
    WEB("Web");
    
    private final String displayName;
    
    DeviceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

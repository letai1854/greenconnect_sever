package com.greenconnect.greenconnect_api.dto.reports.analytical;

/**
 * Enum cho các khoảng thời gian preset
 * Đồng bộ 100% với Flutter client TimePreset
 * 
 * Client gửi: "SEVEN_DAYS", "THIRTY_DAYS", etc.
 */
public enum TimePreset {
    SEVEN_DAYS,      // 7 ngày gần nhất
    THIRTY_DAYS,     // 30 ngày gần nhất (default)
    NINETY_DAYS,     // 90 ngày gần nhất 
    THIS_YEAR,       // Năm hiện tại
    CUSTOM;          // Tùy chọn (dùng startDate/endDate)
    
    /**
     * Parse string to TimePreset enum
     * Client gửi: "SEVEN_DAYS" -> SEVEN_DAYS
     */
    public static TimePreset fromValue(String value) {
        if (value == null || value.isBlank()) {
            return THIRTY_DAYS;
        }
        
        try {
            return TimePreset.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return THIRTY_DAYS; // Default fallback
        }
    }
}

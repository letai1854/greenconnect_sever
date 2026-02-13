package com.greenconnect.greenconnect_api.enums;

public enum VoucherStatus {
    AVAILABLE,      // Có thể sử dụng ngay
    UPCOMING,       // Sắp diễn ra (chưa đến ngày bắt đầu)
    EXPIRED,        // Đã hết hạn
    OUT_OF_STOCK,   // Hết lượt sử dụng
    USED,           // Đã sử dụng bởi user này
    INACTIVE        // Không hoạt động
}
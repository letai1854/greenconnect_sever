package com.greenconnect.greenconnect_api.enums;

/**
 * Enum cho phương thức thanh toán ưu tiên của user.
 * <p>User có thể chọn phương thức thanh toán mặc định khi đặt hàng.</p>
 */
public enum PaymentMethod {
    /**
     * Thanh toán khi nhận hàng (Cash On Delivery) - Mặc định cho customer mới
     */
    COD,
    
    /**
     * Thanh toán qua VNPay
     */
    VNPAY,
    
    /**
     * Thanh toán qua MoMo
     */
    MOMO,
    
    /**
     * Thanh toán qua VietQR
     */
    VIETQR
}
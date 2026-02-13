package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VnpayPaymentResponse {
    
    private UUID orderId;
    private String orderCode;
    private String paymentUrl;
    private String message;
    private boolean success;
    private BigDecimal totalAmount;
    
    // ========== VNPay TRANSACTION INFO ==========
    private String vnpTxnRef; // Mã tham chiếu giao dịch
    private String vnpOrderInfo; // Thông tin đơn hàng
    private Long vnpAmount; // Số tiền (VND * 100)
    private String vnpCreateDate; // Thời gian tạo
}
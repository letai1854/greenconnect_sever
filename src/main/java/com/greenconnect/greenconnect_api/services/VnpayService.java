package com.greenconnect.greenconnect_api.services;

import java.math.BigDecimal;
import java.util.Map;

/**
 * VNPay Payment Service Interface
 * 
 * <p><b>🔒 VNPay Security & Integration Service</b></p>
 * <p>Xử lý tất cả logic liên quan đến VNPay payment gateway:</p>
 * <ul>
 *   <li>🔗 Tạo payment URL với tất cả parameters bắt buộc</li>
 *   <li>🔐 Tạo và xác thực secure hash (chữ ký số)</li>
 *   <li>✅ Validate callback/return từ VNPay</li>
 *   <li>📊 Parse payment result</li>
 * </ul>
 * 
 * <p><b>⚡ Key Features:</b></p>
 * <ul>
 *   <li>Auto-generate all required VNPay parameters</li>
 *   <li>Secure hash validation với SHA256</li>
 *   <li>Proper URL encoding theo VNPay specs</li>
 *   <li>Error handling cho invalid responses</li>
 * </ul>
 */
public interface VnpayService {

    /**
     * Tạo VNPay payment URL với tất cả parameters bắt buộc
     * 
     * @param orderCode Mã đơn hàng (vnp_TxnRef)
     * @param amount Số tiền thanh toán (VND)
     * @param orderInfo Thông tin đơn hàng  
     * @param clientIpAddress IP address của client
     * @return Complete VNPay payment URL với secure hash
     */
    String createPaymentUrl(String orderCode, BigDecimal amount, String orderInfo, String clientIpAddress);

    /**
     * Xác thực secure hash từ VNPay response
     * 
     * @param vnpayParams All parameters từ VNPay callback/return
     * @return true nếu hash hợp lệ, false nếu không
     */
    boolean validateSecureHash(Map<String, String> vnpayParams);

    /**
     * Parse payment result từ VNPay response
     * 
     * @param vnpayParams Parameters từ VNPay
     * @return Payment result info
     */
    VnpayPaymentResult parsePaymentResult(Map<String, String> vnpayParams);

    /**
     * 🔄 Yêu cầu hoàn tiền (Refund) VNPay
     * 
     * <p><b>VNPay Refund API - Server-to-Server</b></p>
     * 
     * @param txnRef Mã đơn hàng (vnp_TxnRef gốc khi thanh toán)
     * @param transactionDate Thời gian giao dịch gốc (yyyyMMddHHmmss) - vnp_CreateDate của payment
     * @param amount Số tiền hoàn (VND) - nhỏ hơn hoặc bằng số tiền gốc
     * @param transactionType "02" (hoàn toàn phần) hoặc "03" (hoàn một phần)
     * @param userCreate Người thực hiện hoàn tiền (email/username admin)
     * @param refundReason Lý do hoàn tiền
     * @param transactionNo Mã GD tại VNPay (optional - nếu có thì truyền vào)
     * @return true nếu ResponseCode = "00" (thành công), false nếu thất bại
     */
    boolean refundTransaction(
        String txnRef,
        String transactionDate, 
        BigDecimal amount,
        String transactionType,
        String userCreate,
        String refundReason,
        String transactionNo
    );

    /**
     * VNPay Payment Result
     */
    record VnpayPaymentResult(
        String orderCode,
        String responseCode,
        String transactionNo,
        BigDecimal amount,
        String bankCode,
        String payDate,
        boolean isSuccess
    ) {}
}
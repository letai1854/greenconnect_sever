package com.greenconnect.greenconnect_api.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VNPay Configuration Properties
 * 
 * <p><b>🔧 CẤU HÌNH VNPAY:</b></p>
 * <p>Load tất cả thông tin VNPay từ application.yml</p>
 * 
 * <p><b>🔒 BẢO MẬT:</b></p>
 * <ul>
 *   <li>⚠️ <b>hashSecret</b> - TUYỆT ĐỐI GIỮ BÍ MẬT!</li>
 *   <li>✅ <b>tmnCode</b> - Public, không sao</li>
 *   <li>🌐 <b>URLs</b> - Phải chính xác, khớp với đăng ký VNPay</li>
 * </ul>
 * 
 * <p><b>📋 SỬ DỤNG:</b></p>
 * <pre>
 * {@code
 * @Autowired
 * private VnpayConfig vnpayConfig;
 * 
 * String url = vnpayConfig.getUrl();
 * String secret = vnpayConfig.getHashSecret();
 * }
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
@Slf4j
public class VnpayConfig {
    
    @PostConstruct
    public void logConfig() {
        log.info("========================================");
        log.info("🔧 VNPAY CONFIGURATION LOADED:");
        log.info("   Gateway URL: {}", url);
        log.info("   TMN Code: {}", tmnCode);
        log.info("   Hash Secret: {}****", hashSecret != null ? hashSecret.substring(0, Math.min(4, hashSecret.length())) : "NULL");
        log.info("   ✅ Return URL (Frontend): {}", returnUrl);
        log.info("   ✅ Callback URL (Backend): {}", callbackUrl);
        log.info("   Version: {}", version);
        log.info("   Locale: {}", locale);
        log.info("========================================");
    }
    
    // ========== VNPAY GATEWAY URLS ==========
    /**
     * VNPay Gateway URL
     * <p>Sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html</p>
     * <p>Production: https://vnpay.vn/paymentv2/vpcpay.html</p>
     */
    private String url;
    
    // ========== MERCHANT CONFIGURATION ==========
    /**
     * Mã website/merchant do VNPay cấp
     * <p>Ví dụ: VNPAY12345678</p>
     */
    private String tmnCode;
    
    /**
     * Khóa bí mật do VNPay cấp
     * <p><b>⚠️ TUYỆT ĐỐI GIỮ BÍ MẬT!</b></p>
     * <p>Dùng để tạo và xác thực chữ ký số</p>
     */
    private String hashSecret;
    
    // ========== CALLBACK URLS ==========
    /**
     * URL Frontend nhận kết quả thanh toán
     * <p>User sẽ được VNPay redirect về URL này</p>
     * <p>Ví dụ: http://localhost:3000/payment-result</p>
     */
    private String returnUrl;
    
    /**
     * URL Backend nhận IPN callback
     * <p>VNPay server sẽ gọi trực tiếp URL này</p>
     * <p>Ví dụ: http://yourserver.com/orders/vnpay-callback</p>
     */
    private String callbackUrl;
    
    // ========== PAYMENT CONFIGURATION ==========
    /**
     * VNPay API version - Mặc định: "2.1.0"
     */
    private String version = "2.1.0";
    
    /**
     * Lệnh thanh toán - Mặc định: "pay"
     */
    private String command = "pay";
    
    /**
     * Loại đơn hàng - Mặc định: "other"
     * <p>Các giá trị khác: billpayment, fashion, etc.</p>
     */
    private String orderType = "other";
    
    /**
     * Đơn vị tiền tệ - Mặc định: "VND"
     */
    private String currencyCode = "VND";
    
    /**
     * Ngôn ngữ giao diện - Mặc định: "vn"
     * <p>Giá trị khác: "en"</p>
     */
    private String locale = "vn";
    
    /**
     * IP Address của server - Dùng cho refund/querydr API
     * <p>Mặc định: "127.0.0.1"</p>
     * <p>Production: IP thật của server</p>
     */
    private String ipAddr = "127.0.0.1";
    
    // ========== TIMEOUT & SECURITY ==========
    /**
     * Timeout thanh toán (giây) - Mặc định: 900 (15 phút)
     */
    private Integer timeout = 900;
    
    /**
     * Loại hash - Mặc định: "SHA512" (VNPay requires SHA512)
     * <p>Các giá trị khác: SHA256, MD5, SHA1</p>
     */
    private String secureHashType = "SHA512";
}
package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.config.VnpayConfig;
import com.greenconnect.greenconnect_api.services.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VNPay Service Implementation
 * 
 * <p><b>ðŸ”’ Comprehensive VNPay Integration</b></p>
 * <p>Implements full VNPay payment gateway integration vá»›i:</p>
 * <ul>
 *   <li>âœ… Complete parameter generation theo VNPay API specs</li>
 *   <li>ðŸ” SHA512 secure hash generation & validation (VNPay requirement)</li>
 *   <li>ðŸ”— Proper URL encoding cho special characters</li>
 *   <li>â° Automatic timestamp generation</li>
 *   <li>ðŸ›¡ï¸ Security validation cho all responses</li>
 * </ul>
 * 
 * <p><b>ðŸŽ¯ VNPay Required Parameters:</b></p>
 * <ul>
 *   <li>vnp_Version, vnp_Command, vnp_TmnCode</li>
 *   <li>vnp_Amount, vnp_CurrCode, vnp_TxnRef</li>
 *   <li>vnp_OrderInfo, vnp_OrderType, vnp_Locale</li>
 *   <li>vnp_ReturnUrl, vnp_IpAddr, vnp_CreateDate</li>
 *   <li>vnp_SecureHash (Generated from all above)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayServiceImpl implements VnpayService {

    private final VnpayConfig vnpayConfig;

    @Override
    public String createPaymentUrl(String orderCode, BigDecimal amount, String orderInfo, String clientIpAddress) {
        try {
            log.info("Creating VNPay payment URL for order: {}, amount: {}", orderCode, amount);
            
            // 1. Build all required VNPay parameters
            Map<String, String> vnpParams = new TreeMap<>(); // TreeMap for sorted keys
            
            // Required parameters theo VNPay API documentation
            vnpParams.put("vnp_Version", vnpayConfig.getVersion());
            vnpParams.put("vnp_Command", vnpayConfig.getCommand());
            vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            
            // â­ FIX: Round to nearest integer before multiplying by 100
            // VNPay does NOT accept decimal amounts (e.g., 807957.5 VND is invalid)
            long amountInCents = amount.setScale(0, java.math.RoundingMode.HALF_UP)
                                       .multiply(BigDecimal.valueOf(100))
                                       .longValue();
            vnpParams.put("vnp_Amount", String.valueOf(amountInCents)); // Convert to VND cents
            
            vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrencyCode());
            vnpParams.put("vnp_TxnRef", orderCode);
            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", vnpayConfig.getOrderType());
            vnpParams.put("vnp_Locale", vnpayConfig.getLocale());
            vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", clientIpAddress);
            
            // â­ ThÃªm CreateDate vÃ  ExpireDate (Báº®T BUá»˜C theo docs VNPay)
            String createDate = getCurrentTimestamp();
            vnpParams.put("vnp_CreateDate", createDate);
            vnpParams.put("vnp_ExpireDate", getExpireTimestamp(createDate)); // 15 phÃºt sau
            
            // 2. Generate secure hash
            log.info("ðŸ“‹ VNPay Parameters BEFORE hash (sorted by TreeMap):");
            vnpParams.forEach((key, value) -> log.info("   {}={}", key, value));
            
            String secureHash = generateSecureHash(vnpParams);
            vnpParams.put("vnp_SecureHash", secureHash);
            
            // 3. Build payment URL
            String paymentUrl = buildPaymentUrl(vnpParams);
            
            log.info("VNPay payment URL created successfully for order: {}", orderCode);
            log.info("ðŸ”— vnp_ReturnUrl configured: {}", vnpayConfig.getReturnUrl());
            log.info("ðŸ”— Full payment URL: {}", paymentUrl);
            
            return paymentUrl;
            
        } catch (Exception e) {
            log.error("Failed to create VNPay payment URL for order: {}", orderCode, e);
            throw new RuntimeException("Failed to create VNPay payment URL", e);
        }
    }

    @Override
    public boolean validateSecureHash(Map<String, String> vnpayParams) {
        try {
            String receivedHash = vnpayParams.get("vnp_SecureHash");
            if (receivedHash == null || receivedHash.isEmpty()) {
                log.warn("Missing vnp_SecureHash in VNPay response");
                return false;
            }
            
            // Remove hash from params for validation
            Map<String, String> paramsForHash = new TreeMap<>(vnpayParams);
            paramsForHash.remove("vnp_SecureHash");
            paramsForHash.remove("vnp_SecureHashType");
            
            String expectedHash = generateSecureHash(paramsForHash);
            boolean isValid = receivedHash.equalsIgnoreCase(expectedHash);
            
            log.info("VNPay hash validation result: {}", isValid);
            if (!isValid) {
                log.warn("Hash mismatch - Expected: {}, Received: {}", expectedHash, receivedHash);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Failed to validate VNPay secure hash", e);
            return false;
        }
    }

    @Override
    public VnpayPaymentResult parsePaymentResult(Map<String, String> vnpayParams) {
        try {
            String orderCode = vnpayParams.get("vnp_TxnRef");
            String responseCode = vnpayParams.get("vnp_ResponseCode");
            String transactionNo = vnpayParams.get("vnp_TransactionNo");
            String amountStr = vnpayParams.get("vnp_Amount");
            String bankCode = vnpayParams.get("vnp_BankCode");
            String payDate = vnpayParams.get("vnp_PayDate");
            
            BigDecimal amount = amountStr != null ? 
                new BigDecimal(amountStr).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
            
            boolean isSuccess = "00".equals(responseCode);
            
            log.info("Parsed VNPay result - Order: {}, Success: {}, ResponseCode: {}", 
                    orderCode, isSuccess, responseCode);
            
            return new VnpayPaymentResult(
                orderCode,
                responseCode,
                transactionNo,
                amount,
                bankCode,
                payDate,
                isSuccess
            );
            
        } catch (Exception e) {
            log.error("Failed to parse VNPay payment result", e);
            throw new RuntimeException("Failed to parse VNPay payment result", e);
        }
    }

    /**
     * Generate secure hash cho VNPay parameters theo chuáº©n VNPay 2.1.0
     */
    private String generateSecureHash(Map<String, String> params) throws Exception {
        // 1. Build hash data string (sorted by key) - VNPay 2.1.0 yÃªu cáº§u URL encode
        StringBuilder hashData = new StringBuilder();
        
        // â­ CRITICAL: VNPay 2.1.0 requires URL encoding for BOTH key and value in hashdata
        params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (hashData.length() > 0) {
                        hashData.append("&");
                    }
                    try {
                        // Encode both key and value theo VNPay docs 2.1.0
                        hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
                                .append("=")
                                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
                    } catch (Exception e) {
                        hashData.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                });
        
        // 2. Generate HMAC-SHA512 hash (VNPay requires SHA512, not SHA256!)
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(
                vnpayConfig.getHashSecret().getBytes(StandardCharsets.UTF_8), 
                "HmacSHA512"
        );
        mac.init(secretKey);
        
        byte[] hashBytes = mac.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
        
        // 3. Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        String result = hexString.toString().toUpperCase();
        log.info("ðŸ” VNPay Hash Generation Debug:");
        log.info("   Hash Secret: {}", vnpayConfig.getHashSecret());
        log.info("   Hash Data: {}", hashData.toString());
        log.info("   Generated Hash: {}", result);
        
        return result;
    }

    /**
     * Build complete payment URL vá»›i all parameters
     * âš ï¸ CRITICAL: vnp_SecureHash MUST be the last parameter in URL
     */
    private String buildPaymentUrl(Map<String, String> params) throws Exception {
        StringBuilder url = new StringBuilder(vnpayConfig.getUrl());
        url.append("?");
        
        // â­ Extract secureHash to add it at the end
        String secureHash = params.get("vnp_SecureHash");
        
        log.info("ðŸ”¨ Building final payment URL...");
        
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            // Skip vnp_SecureHash - will add at the end
            if ("vnp_SecureHash".equals(entry.getKey())) {
                continue;
            }
            
            if (!first) {
                url.append("&");
            }
            url.append(entry.getKey())
               .append("=")
               .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            first = false;
        }
        
        // â­ Add vnp_SecureHash at the very end (MUST be last parameter)
        if (secureHash != null) {
            url.append("&vnp_SecureHash=").append(secureHash); // Already in hex, no encoding needed
            log.info("âœ… vnp_SecureHash appended as LAST parameter");
        }
        
        return url.toString();
    }

    /**
     * Get current timestamp trong format VNPay yÃªu cáº§u: yyyyMMddHHmmss
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * Get expire timestamp (15 minutes from create date)
     */
    private String getExpireTimestamp(String createDate) {
        try {
            LocalDateTime createDateTime = LocalDateTime.parse(createDate, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            LocalDateTime expireDateTime = createDateTime.plusMinutes(15);
            return expireDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            log.warn("Failed to parse create date, using default expire time: {}", e.getMessage());
            return LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
    }

    /**
     * ðŸ”„ YÃªu cáº§u hoÃ n tiá»n (Refund) VNPay
     * 
     * <p><b>VNPay Refund API - Server-to-Server</b></p>
     * 
     * @param txnRef MÃ£ Ä‘Æ¡n hÃ ng (vnp_TxnRef gá»‘c khi thanh toÃ¡n)
     * @param transactionDate Thá»i gian giao dá»‹ch gá»‘c (yyyyMMddHHmmss) - vnp_CreateDate cá»§a payment
     * @param amount Sá»‘ tiá»n hoÃ n (VND) - nhá» hÆ¡n hoáº·c báº±ng sá»‘ tiá»n gá»‘c
     * @param transactionType "02" (hoÃ n toÃ n pháº§n) hoáº·c "03" (hoÃ n má»™t pháº§n)
     * @param userCreate NgÆ°á»i thá»±c hiá»‡n hoÃ n tiá»n (email/username admin)
     * @param refundReason LÃ½ do hoÃ n tiá»n
     * @param transactionNo MÃ£ GD táº¡i VNPay (optional - náº¿u cÃ³ thÃ¬ truyá»n vÃ o)
     * @return true náº¿u ResponseCode = "00" (thÃ nh cÃ´ng), false náº¿u tháº¥t báº¡i
     */
    @Override
    public boolean refundTransaction(
            String txnRef,
            String transactionDate, 
            BigDecimal amount,
            String transactionType,
            String userCreate,
            String refundReason,
            String transactionNo) {
        
        try {
            log.info("ðŸ”„ [VNPAY REFUND] Báº¯t Ä‘áº§u yÃªu cáº§u hoÃ n tiá»n - TxnRef: {}, Amount: {}", txnRef, amount);
            
            // 1. Validate inputs
            if (txnRef == null || transactionDate == null || amount == null || userCreate == null) {
                log.error("âŒ [VNPAY REFUND] Thiáº¿u tham sá»‘ báº¯t buá»™c");
                return false;
            }
            
            // 2. Generate unique request ID (duy nhất trong ngày)
            String requestId = generateUniqueRequestId();
            String createDate = getCurrentTimestamp();
            String ipAddr = vnpayConfig.getIpAddr(); // ← Load from config instead of hardcode
            
            // 3. Prepare parameters
            String type = transactionType != null ? transactionType : "02"; // Default: full refund
            long amountInCents = amount.setScale(0, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
            
            String orderInfo = refundReason != null ? refundReason : "Hoan tien don hang " + txnRef;
            String transNo = transactionNo != null ? transactionNo : "";
            
            // 4. Build checksum data theo VNPay docs
            // data = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" + 
            //        vnp_TransactionType + "|" + vnp_TxnRef + "|" + vnp_Amount + "|" + vnp_TransactionNo + "|" + 
            //        vnp_TransactionDate + "|" + vnp_CreateBy + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" + vnp_OrderInfo
            String checksumData = requestId + "|" +
                vnpayConfig.getVersion() + "|" +
                "refund" + "|" +
                vnpayConfig.getTmnCode() + "|" +
                type + "|" +
                txnRef + "|" +
                amountInCents + "|" +
                transNo + "|" +
                transactionDate + "|" +
                userCreate + "|" +
                createDate + "|" +
                ipAddr + "|" +
                orderInfo;
            
            String secureHash = generateHmacSHA512(checksumData, vnpayConfig.getHashSecret());
            
            log.info("ðŸ“‹ [VNPAY REFUND] Checksum Data: {}", checksumData);
            log.info("ðŸ” [VNPAY REFUND] SecureHash: {}", secureHash);
            
            // 5. Build JSON request body
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("vnp_RequestId", requestId);
            requestBody.put("vnp_Version", vnpayConfig.getVersion());
            requestBody.put("vnp_Command", "refund");
            requestBody.put("vnp_TmnCode", vnpayConfig.getTmnCode());
            requestBody.put("vnp_TransactionType", type);
            requestBody.put("vnp_TxnRef", txnRef);
            requestBody.put("vnp_Amount", amountInCents);
            requestBody.put("vnp_OrderInfo", orderInfo);
            requestBody.put("vnp_TransactionDate", transactionDate);
            requestBody.put("vnp_CreateBy", userCreate);
            requestBody.put("vnp_CreateDate", createDate);
            requestBody.put("vnp_IpAddr", ipAddr);
            
            if (transactionNo != null && !transactionNo.isEmpty()) {
                requestBody.put("vnp_TransactionNo", transactionNo);
            }
            
            requestBody.put("vnp_SecureHash", secureHash);
            
            log.info("ðŸ“¤ [VNPAY REFUND] Request Body:");
            requestBody.forEach((key, value) -> log.info("   {} = {}", key, value));
            
            // 6. Call VNPay API
            String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
            String jsonRequest = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestBody);
            
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(apiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 7. Read response
            int responseCode = conn.getResponseCode();
            StringBuilder responseBody = new StringBuilder();
            
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(
                        responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), 
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    responseBody.append(line.trim());
                }
            }
            
            log.info("ðŸ“¥ [VNPAY REFUND] Response Status: {}", responseCode);
            log.info("ðŸ“¥ [VNPAY REFUND] Response Body: {}", responseBody.toString());
            
            // 8. Parse JSON response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(responseBody.toString(), Map.class);
            
            // 9. Validate response secure hash
            String receivedHash = (String) responseMap.get("vnp_SecureHash");
            String responseHashData = responseMap.get("vnp_ResponseId") + "|" +
                responseMap.getOrDefault("vnp_Command", "") + "|" +
                responseMap.get("vnp_ResponseCode") + "|" +
                responseMap.get("vnp_Message") + "|" +
                responseMap.getOrDefault("vnp_TmnCode", "") + "|" +
                responseMap.get("vnp_TxnRef") + "|" +
                responseMap.get("vnp_Amount") + "|" +
                responseMap.getOrDefault("vnp_BankCode", "") + "|" +
                responseMap.getOrDefault("vnp_PayDate", "") + "|" +
                responseMap.get("vnp_TransactionNo") + "|" +
                responseMap.get("vnp_TransactionType") + "|" +
                responseMap.get("vnp_TransactionStatus") + "|" +
                responseMap.get("vnp_OrderInfo");
            
            String expectedHash = generateHmacSHA512(responseHashData, vnpayConfig.getHashSecret());
            
            if (!receivedHash.equalsIgnoreCase(expectedHash)) {
                log.error("âŒ [VNPAY REFUND] Response hash validation FAILED!");
                log.error("   Expected: {}", expectedHash);
                log.error("   Received: {}", receivedHash);
                return false;
            }
            
            log.info("âœ… [VNPAY REFUND] Response hash validated successfully");
            
            // 10. Check response code
            String vnpResponseCode = (String) responseMap.get("vnp_ResponseCode");
            String message = (String) responseMap.get("vnp_Message");
            
            log.info("ðŸ“Š [VNPAY REFUND] ResponseCode: {}, Message: {}", vnpResponseCode, message);
            
            boolean success = "00".equals(vnpResponseCode);
            
            if (success) {
                log.info("âœ… [VNPAY REFUND] HoÃ n tiá»n thÃ nh cÃ´ng - TxnRef: {}, TransactionNo: {}", 
                    txnRef, responseMap.get("vnp_TransactionNo"));
            } else {
                log.warn("âŒ [VNPAY REFUND] HoÃ n tiá»n tháº¥t báº¡i - ResponseCode: {}, Message: {}", 
                    vnpResponseCode, message);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("âŒ [VNPAY REFUND] Exception: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate unique request ID (format: yyyyMMddHHmmss + random 6 digits)
     * Äáº£m báº£o duy nháº¥t trong ngÃ y theo yÃªu cáº§u VNPay
     */
    private String generateUniqueRequestId() {
        String timestamp = getCurrentTimestamp();
        int random = (int) (Math.random() * 900000) + 100000; // 6 digits (100000-999999)
        return timestamp + random;
    }

    /**
     * Generate HMAC-SHA512 hash tá»« raw string data
     * DÃ¹ng cho API refund/querydr theo chuáº©n VNPay
     * 
     * @param data Raw data string (cÃ¡c field ná»‘i bá»Ÿi "|")
     * @param secretKey Hash secret tá»« VNPay
     * @return Hex string uppercase
     */
    private String generateHmacSHA512(String data, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secretKey.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA512"
        );
        mac.init(secretKeySpec);
        
        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString().toUpperCase();
    }
}

package com.greenconnect.greenconnect_api.utils;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for TOTP (Time-based One-Time Password) operations.
 * <p>Implements RFC 6238 standard for TOTP generation and validation.</p>
 */
@Slf4j
public class TotpUtils {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final int SECRET_KEY_LENGTH = 20; // 160 bits
    private static final int TIME_STEP = 30; // 30 seconds
    private static final int OTP_LENGTH = 6; // 6 digits

    /**
     * Tạo secret key ngẫu nhiên cho TOTP.
     * <p>Secret key này sẽ được lưu vào database và dùng để tạo QR code.</p>
     *
     * @return Base32 encoded secret key
     */
    public static String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_KEY_LENGTH];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Tạo URL cho QR code Google Authenticator.
     * <p>Format: otpauth://totp/GreenConnect:email?secret=SECRET&issuer=GreenConnect</p>
     *
     * @param email Email của user
     * @param secretKey Secret key (Base32 encoded)
     * @return QR code URL
     */
    public static String generateQrCodeUrl(String email, String secretKey) {
        String issuer = "GreenConnect";
        String label = issuer + ":" + email;
        
        // Encode secret key to Base32 for Google Authenticator compatibility
        String base32Secret = toBase32(secretKey);
        
        return String.format(
            "otpauth://totp/%s?secret=%s&issuer=%s",
            label,
            base32Secret,
            issuer
        );
    }

    /**
     * Xác thực OTP code với secret key.
     * <p>Cho phép sai số ±1 time window (90 seconds total).</p>
     *
     * @param otpCode OTP code từ user (6 digits)
     * @param secretKey Secret key của user
     * @return true nếu OTP hợp lệ
     */
    public static boolean verifyOtp(String otpCode, String secretKey) {
        if (otpCode == null || otpCode.length() != OTP_LENGTH) {
            return false;
        }

        try {
            int code = Integer.parseInt(otpCode);
            long currentTime = System.currentTimeMillis() / 1000 / TIME_STEP;

            // Check current time window and ±1 window (for clock skew)
            for (int i = -1; i <= 1; i++) {
                int generatedCode = generateTotpCode(secretKey, currentTime + i);
                if (generatedCode == code) {
                    log.info("✅ OTP verified successfully (time window: {})", i);
                    return true;
                }
            }

            log.warn("❌ OTP verification failed");
            return false;

        } catch (NumberFormatException e) {
            log.warn("❌ Invalid OTP format: {}", otpCode);
            return false;
        } catch (Exception e) {
            log.error("❌ Error verifying OTP: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tạo TOTP code từ secret key và time counter.
     *
     * @param secretKey Secret key (Base64 encoded)
     * @param timeCounter Time counter (current time / 30)
     * @return 6-digit OTP code
     */
    private static int generateTotpCode(String secretKey, long timeCounter) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(secretKey);
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeCounter).array();

            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            SecretKeySpec signKey = new SecretKeySpec(secretBytes, HMAC_SHA1_ALGORITHM);
            mac.init(signKey);

            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1000000;
            return otp;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating TOTP code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }

    /**
     * Convert Base64 string to Base32 (for Google Authenticator compatibility).
     * <p>Base32 alphabet: A-Z, 2-7</p>
     *
     * @param base64String Base64 encoded string
     * @return Base32 encoded string
     */
    private static String toBase32(String base64String) {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        String base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < bytes.length; i += 5) {
            long buffer = 0;
            int bitsAvailable = 0;

            for (int j = 0; j < 5 && i + j < bytes.length; j++) {
                buffer = (buffer << 8) | (bytes[i + j] & 0xFF);
                bitsAvailable += 8;
            }

            while (bitsAvailable >= 5) {
                bitsAvailable -= 5;
                int index = (int) ((buffer >> bitsAvailable) & 0x1F);
                result.append(base32Alphabet.charAt(index));
            }

            if (bitsAvailable > 0) {
                int index = (int) ((buffer << (5 - bitsAvailable)) & 0x1F);
                result.append(base32Alphabet.charAt(index));
            }
        }

        return result.toString();
    }
}

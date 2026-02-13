package com.greenconnect.greenconnect_api.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Dotenv Configuration
 * 
 * <p><b>🔒 TẢI BIẾN MÔI TRƯỜNG TỪ .env FILE:</b></p>
 * <p>Load các thông tin nhạy cảm từ .env file vào System Properties</p>
 * 
 * <p><b>🛡️ BẢO MẬT:</b></p>
 * <ul>
 *   <li>✅ Tách biệt thông tin nhạy cảm khỏi code</li>
 *   <li>✅ Tránh commit secrets lên Git</li>
 *   <li>✅ Khác nhau theo environment (dev/staging/prod)</li>
 * </ul>
 * 
 * <p><b>📁 CẤU TRÚC FILE:</b></p>
 * <pre>
 * .env.example  ← Template cho team
 * .env          ← File thật (không commit)
 * .env.local    ← Local development (không commit)
 * </pre>
 */
@Configuration
@Slf4j
public class DotenvConfig {

    /**
     * Load .env file khi ứng dụng khởi động
     * 
     * <p><b>🔄 THỨ TỰ TÌM KIẾM:</b></p>
     * <ol>
     *   <li>.env.local (highest priority)</li>
     *   <li>.env</li>
     *   <li>System environment variables (lowest priority)</li>
     * </ol>
     */
    @PostConstruct
    public void loadEnvironmentVariables() {
        try {
            // Tìm .env file ở thư mục gốc của project
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")           // Thư mục chứa .env
                    .filename(".env")          // Tên file
                    .ignoreIfMalformed()       // Bỏ qua nếu file bị lỗi format
                    .ignoreIfMissing()         // Bỏ qua nếu không tìm thấy file
                    .systemProperties()        // Load vào System Properties
                    .load();

            log.info("🔧 Loaded .env file successfully");

            // Log các biến đã load (CHỈ TÊN, KHÔNG GIẮT TRỊ)
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Mask sensitive values for logging
                String maskedValue = maskSensitiveValue(key, value);
                log.debug("🔑 Loaded env var: {} = {}", key, maskedValue);
            });

        } catch (Exception e) {
            log.warn("⚠️ Could not load .env file: {}. Using system environment variables.", e.getMessage());
        }
    }

    /**
     * Che giấu giá trị nhạy cảm khi log
     * 
     * @param key Tên biến
     * @param value Giá trị gốc
     * @return Giá trị đã che giấu
     */
    private String maskSensitiveValue(String key, String value) {
        if (value == null || value.isEmpty()) {
            return "[EMPTY]";
        }

        // Danh sách các key nhạy cảm
        String[] sensitiveKeys = {
                "SECRET", "PASSWORD", "HASH", "KEY", "TOKEN", 
                "PRIVATE", "CREDENTIAL", "AUTH"
        };

        // Kiểm tra xem key có chứa từ khóa nhạy cảm không
        String upperKey = key.toUpperCase();
        for (String sensitiveKey : sensitiveKeys) {
            if (upperKey.contains(sensitiveKey)) {
                // Hiển thị 3 ký tự đầu + *** + 3 ký tự cuối
                if (value.length() <= 6) {
                    return "***";
                } else {
                    return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
                }
            }
        }

        // Không phải thông tin nhạy cảm - hiển thị bình thường
        return value;
    }
}
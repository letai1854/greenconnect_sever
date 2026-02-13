package com.greenconnect.greenconnect_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.greenconnect.greenconnect_api.services.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled tasks configuration.
 * <p>Xử lý các tác vụ định kỳ như cleanup expired tokens.</p>
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {
    
    private final RefreshTokenService refreshTokenService;
    
    /**
     * Cleanup expired refresh tokens.
     * <p>Chạy mỗi 6 giờ để dọn dẹp database.</p>
     */
    @Scheduled(fixedRate = 21600000) // 6 hours = 6 * 60 * 60 * 1000 ms
    public void cleanupExpiredTokens() {
        log.info("Bắt đầu scheduled cleanup expired refresh tokens");
        
        try {
            int deletedCount = refreshTokenService.cleanupExpiredTokens();
            log.info("Scheduled cleanup completed: Đã xóa {} expired refresh tokens", deletedCount);
        } catch (Exception e) {
            log.error("Lỗi trong quá trình cleanup expired tokens: {}", e.getMessage(), e);
        }
    }
}
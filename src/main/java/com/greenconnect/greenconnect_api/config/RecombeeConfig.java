package com.greenconnect.greenconnect_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.recombee.api_client.RecombeeClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Recombee Configuration
 * Khởi tạo RecombeeClient bean để giao tiếp với Recombee API
 */
@Configuration
@Slf4j
public class RecombeeConfig {
    
    @Value("${recombee.database-id:greenconnect-com-dev}")
    private String databaseId;
    
    @Value("${recombee.private-token:YOUR_PRIVATE_TOKEN}")
    private String privateToken;
    
    @Bean
    public RecombeeClient recombeeClient() {
        log.info("🔧 [RECOMBEE CONFIG] Initializing RecombeeClient for Australia region (ap-se)");
        log.info("📋 [RECOMBEE CONFIG] Database ID: {} | Private Token: {}***", databaseId, 
            privateToken != null && privateToken.length() > 10 ? privateToken.substring(0, 10) : "NULL");
        
        try {
            // Validate config before creating client
            if (privateToken == null || privateToken.equals("YOUR_PRIVATE_TOKEN") || privateToken.trim().isEmpty()) {
                log.error("❌ [RECOMBEE CONFIG] Private token not configured properly!");
                log.warn("⚠️ [RECOMBEE CONFIG] Recombee tracking will be DISABLED. App continues without recommendation engine.");
                return null;  // Return null instead of crashing
            }
            
            if (databaseId == null || databaseId.trim().isEmpty()) {
                log.error("❌ [RECOMBEE CONFIG] Database ID not configured!");
                log.warn("⚠️ [RECOMBEE CONFIG] Recombee tracking will be DISABLED. App continues without recommendation engine.");
                return null;
            }
            
            // ✅ GIẢI PHÁP ĐÚNG: Chỉ truyền hostname, KHÔNG có https://
            // Thư viện sẽ tự động thêm https:// vào trước
            // Format: rapi-{region}.recombee.com (KHÔNG có https://)
            
            RecombeeClient client = new RecombeeClient(
                databaseId, 
                privateToken
            ).setBaseUri("rapi-ap-se.recombee.com");  // ← Chỉ hostname, không có protocol
            
            log.info("✅ [RECOMBEE CONFIG] RecombeeClient initialized successfully!");
            log.info("   → Database: {}", databaseId);
            log.info("   → BaseUri: rapi-ap-se.recombee.com");
            log.info("   → Region: Australia (AP-SOUTHEAST)");
            log.info("   → Client status: READY ✓");
            
            return client;
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE CONFIG] Failed to initialize RecombeeClient: {}", e.getMessage(), e);
            log.warn("⚠️ [RECOMBEE CONFIG] Recombee tracking will be DISABLED. App continues without recommendation engine.");
            return null;  // Return null instead of crashing the entire app
        }
    }
}

package com.greenconnect.greenconnect_api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.recombee.api_client.RecombeeClient;
import com.recombee.api_client.api_requests.AddItemProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * RecombeeSchemaInitializer
 * 
 * Tự động tạo các Item Properties cần thiết trong Recombee Database
 * Chạy 1 lần khi application khởi động
 * 
 * ⚠️ QUAN TRỌNG: Chạy TRƯỚC KHI sync products
 */
@Component
@Slf4j
public class RecombeeSchemaInitializer implements CommandLineRunner {
    
    @Autowired(required = false)
    private RecombeeClient recombeeClient;
    
    @Override
    public void run(String... args) {
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE SCHEMA] RecombeeClient not available - Schema initialization skipped");
            return;
        }
        
        log.info("🔧 [RECOMBEE SCHEMA] Initializing item properties...");
        
        try {
            // === Product Properties ===
            createPropertyIfNotExists("name", "string");
            createPropertyIfNotExists("category", "string");
            createPropertyIfNotExists("categoryId", "string");
            createPropertyIfNotExists("supplier", "string");
            createPropertyIfNotExists("supplierId", "string");  // ← Fix lỗi này
            createPropertyIfNotExists("price", "double");
            createPropertyIfNotExists("imageUrl", "string");
            createPropertyIfNotExists("rating", "double");
            createPropertyIfNotExists("reviewCount", "int");
            createPropertyIfNotExists("isActive", "boolean");
            createPropertyIfNotExists("isFeatured", "boolean");
            createPropertyIfNotExists("slug", "string");
            
            log.info("✅ [RECOMBEE SCHEMA] Item properties initialized successfully!");
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE SCHEMA] Failed to initialize properties: {}", e.getMessage());
            log.warn("⚠️ [RECOMBEE] Product sync may fail if properties don't exist. Please create them manually in Recombee Dashboard.");
        }
    }
    
    /**
     * Tạo property nếu chưa tồn tại
     * Nếu đã tồn tại → bỏ qua (không crash)
     */
    private void createPropertyIfNotExists(String propertyName, String type) {
        try {
            recombeeClient.send(new AddItemProperty(propertyName, type));
            log.info("   ✅ Created property: {} ({})", propertyName, type);
        } catch (Exception e) {
            // Property đã tồn tại hoặc lỗi khác → bỏ qua
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.debug("   ℹ️ Property already exists: {}", propertyName);
            } else {
                log.warn("   ⚠️ Failed to create property '{}': {}", propertyName, e.getMessage());
            }
        }
    }
}

package com.greenconnect.greenconnect_api.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database migration component to drop old address columns on startup.
 * This ensures compatibility with the new Vietnam address standard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressMigration {
    
    private final JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void migrateAddressFields() {
        try {
            log.info("🔄 Bắt đầu migration: Xóa các cột địa chỉ cũ...");
            
            // Drop old address columns
            jdbcTemplate.execute("ALTER TABLE addresses DROP COLUMN IF EXISTS ward");
            log.info("✅ Đã xóa cột 'ward'");
            
            jdbcTemplate.execute("ALTER TABLE addresses DROP COLUMN IF EXISTS district");
            log.info("✅ Đã xóa cột 'district'");
            
            jdbcTemplate.execute("ALTER TABLE addresses DROP COLUMN IF EXISTS city");
            log.info("✅ Đã xóa cột 'city'");
            
            jdbcTemplate.execute("ALTER TABLE addresses DROP COLUMN IF EXISTS district_new");
            log.info("✅ Đã xóa cột 'district_new'");
            
            jdbcTemplate.execute("ALTER TABLE addresses DROP COLUMN IF EXISTS city_new");
            log.info("✅ Đã xóa cột 'city_new'");
            
            // Make note nullable
            jdbcTemplate.execute("ALTER TABLE addresses MODIFY COLUMN note VARCHAR(255) NULL");
            log.info("✅ Đã set cột 'note' thành nullable");
            
            log.info("✅ Migration hoàn tất: Đã xóa tất cả các cột địa chỉ cũ");
            
        } catch (Exception e) {
            // Nếu cột không tồn tại hoặc đã được xóa rồi, bỏ qua lỗi
            log.warn("⚠️ Migration warning: {} (Có thể các cột đã được xóa trước đó)", e.getMessage());
        }
    }
}

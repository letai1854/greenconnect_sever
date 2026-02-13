-- -- =====================================================
-- -- FIX: Xóa các cột thừa trong footer tables
-- -- Chạy trực tiếp trong MySQL Workbench hoặc CLI
-- -- =====================================================

-- -- 1. Xóa cột slug trong footer_sections (nếu có)
-- ALTER TABLE footer_sections DROP COLUMN IF EXISTS slug;

-- -- 2. Xóa cột label trong footer_links (nếu có)  
-- ALTER TABLE footer_links DROP COLUMN IF EXISTS label;

-- -- =====================================================
-- -- Nếu lệnh DROP COLUMN IF EXISTS không hoạt động (MySQL cũ),
-- -- dùng cách này:
-- -- =====================================================

-- -- SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
-- --                WHERE TABLE_SCHEMA = DATABASE() 
-- --                AND TABLE_NAME = 'footer_sections' 
-- --                AND COLUMN_NAME = 'slug');
-- -- SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE footer_sections DROP COLUMN slug', 'SELECT 1');
-- -- PREPARE stmt FROM @sqlstmt;
-- -- EXECUTE stmt;
-- -- DEALLOCATE PREPARE stmt;

-- -- SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
-- --                WHERE TABLE_SCHEMA = DATABASE() 
-- --                AND TABLE_NAME = 'footer_links' 
-- --                AND COLUMN_NAME = 'label');
-- -- SET @sqlstmt := IF(@exist > 0, 'ALTER TABLE footer_links DROP COLUMN label', 'SELECT 1');
-- -- PREPARE stmt FROM @sqlstmt;
-- -- EXECUTE stmt;
-- -- DEALLOCATE PREPARE stmt;

-- -- =====================================================
-- -- Verify - Kiểm tra sau khi chạy
-- -- =====================================================
-- DESCRIBE footer_sections;
-- DESCRIBE footer_links;

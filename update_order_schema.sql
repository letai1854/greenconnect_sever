-- -- ================================================
-- -- MIGRATION: Update Order Schema
-- -- Date: 2025-11-15
-- -- Changes:
-- --   1. Remove delivery_address_id column
-- --   2. Change voucher_id (single) to order_vouchers table (many-to-many)
-- --   3. Remove delivery_time_slot column
-- -- ================================================

-- USE greenconnect_db;

-- -- ===== STEP 1: Backup dữ liệu voucher hiện tại =====
-- -- Tạo bảng tạm để backup voucher_id trước khi xóa
-- CREATE TABLE IF NOT EXISTS orders_voucher_backup (
--     order_id BINARY(16) NOT NULL,
--     voucher_id BINARY(16) NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );

-- -- Backup dữ liệu voucher_id
-- INSERT INTO orders_voucher_backup (order_id, voucher_id)
-- SELECT id, voucher_id 
-- FROM orders 
-- WHERE voucher_id IS NOT NULL;

-- -- ===== STEP 2: Tạo bảng order_vouchers (Many-to-Many) =====
-- CREATE TABLE IF NOT EXISTS order_vouchers (
--     id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
--     order_id BINARY(16) NOT NULL,
--     voucher_id BINARY(16) NOT NULL,
--     discount_applied DECIMAL(15,2) NOT NULL COMMENT 'Số tiền giảm giá đã áp dụng từ voucher này',
--     applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
--     -- Foreign keys
--     CONSTRAINT fk_order_vouchers_order FOREIGN KEY (order_id) 
--         REFERENCES orders(id) ON DELETE CASCADE,
--     CONSTRAINT fk_order_vouchers_voucher FOREIGN KEY (voucher_id) 
--         REFERENCES vouchers(id) ON DELETE RESTRICT,
    
--     -- Indexes
--     INDEX idx_order_vouchers_order (order_id),
--     INDEX idx_order_vouchers_voucher (voucher_id),
    
--     -- Unique constraint: Một order không thể dùng cùng voucher 2 lần
--     UNIQUE KEY uk_order_voucher (order_id, voucher_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- COMMENT='Bảng quan hệ nhiều-nhiều giữa Order và Voucher (1 order có thể dùng nhiều voucher)';

-- -- ===== STEP 3: Migrate dữ liệu từ orders.voucher_id sang order_vouchers =====
-- INSERT INTO order_vouchers (order_id, voucher_id, discount_applied)
-- SELECT 
--     o.id,
--     o.voucher_id,
--     COALESCE(o.discount_amount, 0) -- Sử dụng discount_amount hiện có
-- FROM orders o
-- WHERE o.voucher_id IS NOT NULL;

-- -- ===== STEP 4: Xóa các cột không dùng nữa =====

-- -- 4.1. Xóa foreign key constraint của voucher_id (nếu có)
-- SET @constraint_name = (
--     SELECT CONSTRAINT_NAME 
--     FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
--     WHERE TABLE_SCHEMA = 'greenconnect_db' 
--         AND TABLE_NAME = 'orders' 
--         AND COLUMN_NAME = 'voucher_id'
--         AND CONSTRAINT_NAME != 'PRIMARY'
--     LIMIT 1
-- );

-- SET @sql = IF(@constraint_name IS NOT NULL,
--     CONCAT('ALTER TABLE orders DROP FOREIGN KEY ', @constraint_name),
--     'SELECT "No FK constraint found for voucher_id" AS info');
    
-- PREPARE stmt FROM @sql;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- -- 4.2. Xóa index của voucher_id (nếu có)
-- SET @index_name = (
--     SELECT INDEX_NAME 
--     FROM INFORMATION_SCHEMA.STATISTICS 
--     WHERE TABLE_SCHEMA = 'greenconnect_db' 
--         AND TABLE_NAME = 'orders' 
--         AND COLUMN_NAME = 'voucher_id'
--         AND INDEX_NAME != 'PRIMARY'
--     LIMIT 1
-- );

-- SET @sql = IF(@index_name IS NOT NULL,
--     CONCAT('ALTER TABLE orders DROP INDEX ', @index_name),
--     'SELECT "No index found for voucher_id" AS info');
    
-- PREPARE stmt FROM @sql;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- -- 4.3. Xóa cột voucher_id
-- ALTER TABLE orders DROP COLUMN IF EXISTS voucher_id;

-- -- 4.4. Xóa cột delivery_address_id
-- ALTER TABLE orders DROP COLUMN IF EXISTS delivery_address_id;

-- -- 4.5. Xóa cột delivery_time_slot
-- ALTER TABLE orders DROP COLUMN IF EXISTS delivery_time_slot;

-- -- ===== STEP 5: Verify changes =====
-- SELECT 
--     'Orders table structure updated' AS status,
--     (SELECT COUNT(*) FROM order_vouchers) AS vouchers_migrated,
--     (SELECT COUNT(*) FROM orders_voucher_backup) AS vouchers_backed_up;

-- -- ===== STEP 6: Cleanup (Uncomment sau khi verify OK) =====
-- -- DROP TABLE IF EXISTS orders_voucher_backup;

-- -- ================================================
-- -- MIGRATION COMPLETED
-- -- ================================================
-- -- ⚠️ LƯU Ý:
-- -- 1. Backup database trước khi chạy migration này!
-- -- 2. Kiểm tra kỹ dữ liệu sau khi migrate
-- -- 3. Xóa bảng orders_voucher_backup sau khi verify OK
-- -- ================================================

-- SELECT '✅ Migration completed successfully!' AS message;

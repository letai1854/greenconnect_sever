-- -- ========================================
-- -- Migration: Create order_vouchers junction table for Many-to-Many relationship
-- -- Version: V002
-- -- Description: Convert single voucher per order to multiple vouchers support
-- -- Date: November 2024
-- -- ========================================

-- -- Step 1: Create backup table for safety
-- CREATE TABLE IF NOT EXISTS orders_voucher_backup (
--     id BINARY(16) PRIMARY KEY,
--     voucher_id BINARY(16),
--     discount_amount DECIMAL(15,2),
--     order_date DATETIME
-- );

-- -- Step 2: Backup existing voucher data
-- INSERT IGNORE INTO orders_voucher_backup (id, voucher_id, discount_amount, order_date)
-- SELECT id, voucher_id, discount_amount, order_date
-- FROM orders
-- WHERE voucher_id IS NOT NULL;

-- -- Step 3: Create order_vouchers junction table
-- CREATE TABLE IF NOT EXISTS order_vouchers (
--     id BINARY(16) PRIMARY KEY,
--     order_id BINARY(16) NOT NULL,
--     voucher_id BINARY(16) NOT NULL,
--     discount_applied DECIMAL(15,2) NOT NULL DEFAULT 0.00,
--     applied_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    
--     -- Unique constraint: prevent duplicate voucher in same order
--     CONSTRAINT uk_order_voucher UNIQUE (order_id, voucher_id),
    
--     -- Foreign Keys
--     CONSTRAINT fk_order_vouchers_order 
--         FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
--     CONSTRAINT fk_order_vouchers_voucher 
--         FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE RESTRICT,
    
--     -- Indexes for performance
--     INDEX idx_order_vouchers_order (order_id),
--     INDEX idx_order_vouchers_voucher (voucher_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -- Step 4: Migrate existing data from orders.voucher_id to order_vouchers
-- INSERT IGNORE INTO order_vouchers (id, order_id, voucher_id, discount_applied, applied_at)
-- SELECT 
--     UNHEX(REPLACE(UUID(), '-', '')) as id,
--     o.id as order_id,
--     o.voucher_id as voucher_id,
--     COALESCE(o.discount_amount, 0.00) as discount_applied,
--     o.order_date as applied_at
-- FROM orders o
-- WHERE o.voucher_id IS NOT NULL;

-- -- Step 5: Drop foreign key constraint for voucher_id (if exists)
-- SET @fk_name = (
--     SELECT CONSTRAINT_NAME 
--     FROM information_schema.KEY_COLUMN_USAGE 
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND TABLE_NAME = 'orders' 
--       AND COLUMN_NAME = 'voucher_id'
--       AND REFERENCED_TABLE_NAME = 'vouchers'
--     LIMIT 1
-- );

-- SET @drop_fk = IF(@fk_name IS NOT NULL, 
--     CONCAT('ALTER TABLE orders DROP FOREIGN KEY ', @fk_name),
--     'SELECT "No FK to drop" as result'
-- );

-- PREPARE stmt FROM @drop_fk;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- -- Step 6: Drop index for voucher_id (if exists)
-- SET @idx_name = (
--     SELECT INDEX_NAME 
--     FROM information_schema.STATISTICS 
--     WHERE TABLE_SCHEMA = DATABASE()
--       AND TABLE_NAME = 'orders' 
--       AND COLUMN_NAME = 'voucher_id'
--       AND INDEX_NAME != 'PRIMARY'
--     LIMIT 1
-- );

-- SET @drop_idx = IF(@idx_name IS NOT NULL, 
--     CONCAT('ALTER TABLE orders DROP INDEX ', @idx_name),
--     'SELECT "No index to drop" as result'
-- );

-- PREPARE stmt FROM @drop_idx;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt;

-- -- Step 7: Drop old columns from orders table
-- ALTER TABLE orders 
--     DROP COLUMN IF EXISTS voucher_id,
--     DROP COLUMN IF EXISTS delivery_address_id,
--     DROP COLUMN IF EXISTS delivery_time_slot;

-- -- ========================================
-- -- Verification: Check migration success
-- -- ========================================
-- -- These are logged for verification purposes

-- -- Count migrated records
-- SELECT 
--     CONCAT('Migrated ', COUNT(*), ' voucher records') as migration_status
-- FROM order_vouchers;

-- -- Verify backup
-- SELECT 
--     CONCAT('Backed up ', COUNT(*), ' orders with vouchers') as backup_status
-- FROM orders_voucher_backup;

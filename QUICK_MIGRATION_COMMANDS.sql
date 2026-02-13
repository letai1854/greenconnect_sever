-- -- =====================================================
-- -- QUICK MIGRATION COMMANDS - Run directly in MySQL
-- -- =====================================================
-- -- Hướng dẫn: Copy từng section và chạy trong MySQL Workbench hoặc command line
-- -- =====================================================

-- USE greenconnect_db;

-- -- =====================================================
-- -- MIGRATION: Order Schema Refactoring (Đơn giản - Chạy từng lệnh)
-- -- Mục đích: Chuyển từ single voucher sang multiple vouchers (Many-to-Many)
-- -- =====================================================

-- USE greenconnect_db;

-- -- =====================================================
-- -- BƯỚC 1: TẮT KIỂM TRA FOREIGN KEY (TẠM THỜI)
-- -- =====================================================
-- SET FOREIGN_KEY_CHECKS = 0;


-- -- =====================================================
-- -- BƯỚC 2: BACKUP DỮ LIỆU CŨ
-- -- =====================================================
-- DROP TABLE IF EXISTS orders_voucher_backup;

-- CREATE TABLE orders_voucher_backup (
--     id BINARY(16) PRIMARY KEY,
--     voucher_id BINARY(16),
--     discount_amount DECIMAL(15,2),
--     order_date DATETIME
-- );

-- INSERT INTO orders_voucher_backup (id, voucher_id, discount_amount, order_date)
-- SELECT id, voucher_id, discount_amount, order_date
-- FROM orders
-- WHERE voucher_id IS NOT NULL;

-- -- Kiểm tra backup
-- SELECT CONCAT('✅ Đã backup ', COUNT(*), ' orders có voucher') as status FROM orders_voucher_backup;


-- -- =====================================================
-- -- BƯỚC 3: TẠO BẢNG order_vouchers MỚI (Không có foreign key trước)
-- -- =====================================================
-- DROP TABLE IF EXISTS order_vouchers;

-- CREATE TABLE order_vouchers (
--     id BINARY(16) PRIMARY KEY,
--     order_id BINARY(16) NOT NULL,
--     voucher_id BINARY(16) NOT NULL,
--     discount_applied DECIMAL(15,2) NOT NULL DEFAULT 0.00,
--     applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
--     UNIQUE KEY uk_order_voucher (order_id, voucher_id),
--     KEY idx_order_vouchers_order (order_id),
--     KEY idx_order_vouchers_voucher (voucher_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- -- =====================================================
-- -- BƯỚC 4: MIGRATE DỮ LIỆU
-- -- =====================================================
-- INSERT INTO order_vouchers (id, order_id, voucher_id, discount_applied, applied_at)
-- SELECT 
--     UNHEX(REPLACE(UUID(), '-', '')) as id,
--     o.id as order_id,
--     o.voucher_id as voucher_id,
--     COALESCE(o.discount_amount, 0.00) as discount_applied,
--     o.order_date as applied_at
-- FROM orders o
-- WHERE o.voucher_id IS NOT NULL;

-- -- Kiểm tra migration
-- SELECT CONCAT('✅ Đã migrate ', COUNT(*), ' records vào order_vouchers') as status FROM order_vouchers;


-- -- =====================================================
-- -- BƯỚC 5: XÓA CÁC COLUMN CŨ TRONG BẢNG orders
-- -- =====================================================
-- -- Xóa trực tiếp (không cần tìm tên FK vì đã tắt kiểm tra)
-- ALTER TABLE orders 
--     DROP COLUMN voucher_id,
--     DROP COLUMN delivery_address_id,
--     DROP COLUMN delivery_time_slot;

-- SELECT '✅ Đã xóa 3 columns: voucher_id, delivery_address_id, delivery_time_slot' as status;


-- -- =====================================================
-- -- BƯỚC 6: THÊM FOREIGN KEY CHO order_vouchers
-- -- =====================================================
-- ALTER TABLE order_vouchers
--     ADD CONSTRAINT fk_order_vouchers_order 
--         FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
--     ADD CONSTRAINT fk_order_vouchers_voucher 
--         FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE RESTRICT;

-- SELECT '✅ Đã thêm foreign keys cho order_vouchers' as status;


-- -- =====================================================
-- -- BƯỚC 7: BẬT LẠI KIỂM TRA FOREIGN KEY
-- -- =====================================================
-- SET FOREIGN_KEY_CHECKS = 1;


-- -- =====================================================
-- -- BƯỚC 8: KIỂM TRA KẾT QUẢ
-- -- =====================================================

-- -- Kiểm tra structure của orders
-- SELECT '📋 Structure bảng orders:' as info;
-- DESCRIBE orders;

-- -- Kiểm tra order_vouchers
-- SELECT '📊 Thống kê order_vouchers:' as info;
-- SELECT 
--     COUNT(*) as total_records,
--     COUNT(DISTINCT order_id) as orders_with_vouchers,
--     COUNT(DISTINCT voucher_id) as unique_vouchers
-- FROM order_vouchers;

-- -- Xem 5 orders có vouchers
-- SELECT '🔍 5 orders đầu tiên có voucher:' as info;
-- SELECT 
--     o.order_code,
--     ov.discount_applied,
--     v.voucher_code,
--     ov.applied_at
-- FROM orders o
-- JOIN order_vouchers ov ON o.id = ov.order_id
-- JOIN vouchers v ON ov.voucher_id = v.id
-- LIMIT 5;

-- -- So sánh backup vs migrated
-- SELECT 
--     (SELECT COUNT(*) FROM orders_voucher_backup) as backup_count,
--     (SELECT COUNT(*) FROM order_vouchers) as migrated_count,
--     CASE 
--         WHEN (SELECT COUNT(*) FROM orders_voucher_backup) = (SELECT COUNT(*) FROM order_vouchers)
--         THEN '✅ KHỚP - Migration thành công!'
--         ELSE '⚠️ KHÔNG KHỚP - Cần kiểm tra!'
--     END as status;


-- -- =====================================================
-- -- BƯỚC 9: DỌN DẸP (Chỉ chạy khi đã chắc chắn OK)
-- -- =====================================================
-- -- DROP TABLE orders_voucher_backup;


-- -- =====================================================
-- -- ROLLBACK (Nếu cần quay lại)
-- -- =====================================================
-- /*
-- -- 1. Tắt FK check
-- SET FOREIGN_KEY_CHECKS = 0;

-- -- 2. Thêm lại column
-- ALTER TABLE orders ADD COLUMN voucher_id BINARY(16) NULL AFTER user_id;
-- ALTER TABLE orders ADD COLUMN delivery_address_id BINARY(16) NULL;
-- ALTER TABLE orders ADD COLUMN delivery_time_slot VARCHAR(50) NULL;

-- -- 3. Restore data
-- UPDATE orders o
-- JOIN orders_voucher_backup b ON o.id = b.id
-- SET o.voucher_id = b.voucher_id;

-- -- 4. Thêm lại FK
-- ALTER TABLE orders 
-- ADD CONSTRAINT fk_orders_voucher 
-- FOREIGN KEY (voucher_id) REFERENCES vouchers(id);

-- -- 5. Xóa bảng mới
-- DROP TABLE order_vouchers;

-- -- 6. Bật lại FK check
-- SET FOREIGN_KEY_CHECKS = 1;
-- */

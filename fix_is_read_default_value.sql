-- -- ============================================================================
-- -- FIX: Add default value for is_read column in notification_recipients
-- -- ============================================================================
-- -- Lỗi: Field 'is_read' doesn't have a default value
-- -- Giải pháp: Thêm DEFAULT FALSE cho cột is_read
-- -- ============================================================================

-- -- Bước 1: Xem cấu trúc hiện tại
-- DESCRIBE notification_recipients;

-- -- Bước 2: Sửa cột is_read để có default value
-- ALTER TABLE notification_recipients 
-- MODIFY COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

-- -- Bước 3: Kiểm tra lại
-- DESCRIBE notification_recipients;

-- -- Bước 4: Test insert không cần chỉ định is_read
-- -- (Chỉ để test, sau đó xóa đi)
-- -- INSERT INTO notification_recipients (id, notification_id, user_id, created_at)
-- -- SELECT 
-- --     gen_random_uuid(),
-- --     (SELECT id FROM notifications LIMIT 1),
-- --     (SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1),
-- --     NOW();

-- -- ============================================================================
-- -- KẾT QUẢ MONG ĐỢI:
-- -- Field         Type        Null    Key     Default
-- -- is_read       tinyint(1)  NO              0
-- -- ============================================================================

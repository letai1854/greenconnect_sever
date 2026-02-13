-- -- ============================================================================
-- -- FORCE DROP is_read column from notifications table
-- -- ============================================================================
-- -- Hibernate không tự động xóa cột khi remove field khỏi entity
-- -- Phải xóa thủ công trong database
-- -- ============================================================================

-- -- Bước 1: Kiểm tra cột is_read có tồn tại không
-- SELECT 
--     COLUMN_NAME, 
--     DATA_TYPE, 
--     IS_NULLABLE, 
--     COLUMN_DEFAULT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = 'defaultdb'
--   AND TABLE_NAME = 'notifications'
--   AND COLUMN_NAME = 'is_read';

-- -- Bước 2: Xóa index liên quan đến is_read (nếu có)
-- -- Kiểm tra indexes
-- SHOW INDEX FROM notifications WHERE Column_name = 'is_read';

-- -- Drop index nếu có (thay tên index thực tế)
-- -- ALTER TABLE notifications DROP INDEX idx_notification_user_read;

-- -- Bước 3: Xóa cột is_read
-- ALTER TABLE notifications DROP COLUMN is_read;

-- -- Bước 4: Kiểm tra lại structure
-- DESCRIBE notifications;

-- -- ============================================================================
-- -- SAU KHI XÓA, RESTART SERVER ĐỂ HIBERNATE KHÔNG TẠO LẠI CỘT NÀY
-- -- ============================================================================

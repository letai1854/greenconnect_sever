-- =====================================================
-- Migration: Xóa cột is_read khỏi bảng notifications
-- Lý do: Trạng thái đọc đã được chuyển sang bảng notification_recipients
--        để hỗ trợ multi-recipient với read status riêng cho từng user
-- =====================================================

-- Bước 1: Drop index sử dụng cột is_read
DROP INDEX IF EXISTS idx_notification_user_read ON notifications;

-- Bước 2: Drop cột is_read
ALTER TABLE notifications DROP COLUMN IF EXISTS is_read;

-- =====================================================
-- ROLLBACK (Nếu cần quay lại)
-- =====================================================

-- Để rollback, chạy các lệnh sau:
/*
-- Thêm lại cột is_read
ALTER TABLE notifications 
ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT false;

-- Tạo lại index
CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read);

-- Đồng bộ dữ liệu từ notification_recipients (nếu cần)
-- Lưu ý: Điều này chỉ hoạt động nếu mỗi notification chỉ có 1 recipient
UPDATE notifications n
SET is_read = (
    SELECT nr.is_read 
    FROM notification_recipients nr 
    WHERE nr.notification_id = n.id 
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1 
    FROM notification_recipients nr 
    WHERE nr.notification_id = n.id
);
*/

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Kiểm tra cấu trúc bảng notifications sau khi migration
DESCRIBE notifications;

-- Kiểm tra tất cả indexes của bảng notifications
SHOW INDEX FROM notifications;

-- Đảm bảo notification_recipients vẫn có cột is_read
SELECT 
    COLUMN_NAME, 
    COLUMN_TYPE, 
    IS_NULLABLE, 
    COLUMN_DEFAULT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'notification_recipients' 
  AND COLUMN_NAME = 'is_read';

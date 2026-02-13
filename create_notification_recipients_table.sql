-- =====================================================
-- Tạo bảng notification_recipients
-- Lưu trữ người nhận và trạng thái đọc của từng thông báo
-- =====================================================

-- Bước 1: Tạo bảng notification_recipients
CREATE TABLE IF NOT EXISTS notification_recipients (
    id BINARY(16) PRIMARY KEY DEFAULT (UUID_TO_BIN(UUID())),
    notification_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_notif_recipient_notification 
        FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_recipient_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique: 1 notification chỉ gửi đến 1 user 1 lần
    CONSTRAINT uk_notification_user UNIQUE (notification_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bước 2: Tạo indexes để tối ưu query
CREATE INDEX idx_notif_recipient_user ON notification_recipients(user_id);
CREATE INDEX idx_notif_recipient_notification ON notification_recipients(notification_id);
CREATE INDEX idx_notif_recipient_user_read ON notification_recipients(user_id, is_read);
CREATE INDEX idx_notif_recipient_created ON notification_recipients(created_at DESC);

-- Bước 3: Migrate dữ liệu từ bảng notifications cũ sang notification_recipients
-- (Tạo recipient cho mỗi notification hiện có)
INSERT INTO notification_recipients (id, notification_id, user_id, is_read, read_at, created_at)
SELECT 
    UUID_TO_BIN(UUID()),
    n.id,
    n.user_id,
    n.is_read,
    CASE WHEN n.is_read = TRUE THEN n.created_at ELSE NULL END,
    n.created_at
FROM notifications n
WHERE NOT EXISTS (
    SELECT 1 FROM notification_recipients nr 
    WHERE nr.notification_id = n.id AND nr.user_id = n.user_id
);

-- Bước 4: Xóa các cột không cần thiết trong bảng notifications
-- (Giữ lại để tham khảo, chạy sau khi đã test kỹ)
-- ALTER TABLE notifications DROP COLUMN user_id;
-- ALTER TABLE notifications DROP COLUMN is_read;

-- Bước 5: Cập nhật indexes cho bảng notifications
-- DROP INDEX idx_notification_user_id ON notifications;
-- DROP INDEX idx_notification_user_read ON notifications;

COMMIT;

-- =====================================================
-- NOTES:
-- =====================================================
-- 1. Bảng notifications giữ lại user_id và is_read để tương thích ngược
-- 2. Có thể drop các cột này sau khi đã migrate hoàn toàn
-- 3. notification_recipients là bảng chính để query trạng thái đọc
-- =====================================================

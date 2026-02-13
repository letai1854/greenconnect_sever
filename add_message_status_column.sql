-- Migration: Thêm trường status vào bảng messages để theo dõi trạng thái tin nhắn
-- Author: System
-- Date: 2025-11-02

-- Thêm cột status vào bảng messages
ALTER TABLE messages
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SENT';

-- Cập nhật tất cả tin nhắn cũ thành SENT (đã lưu thành công)
UPDATE messages 
SET status = 'SENT' 
WHERE status IS NULL OR status = '';

-- Tạo index cho cột status để tối ưu query
CREATE INDEX idx_message_status ON messages(status);

-- Comment cho cột mới
COMMENT ON COLUMN messages.status IS 'Trạng thái gửi tin nhắn: SENT (đã lưu DB thành công), FAILED (lỗi gửi)';

-- Thêm cột recipient vào bảng notifications
-- Sử dụng lệnh này để cập nhật database schema

-- Thêm cột mới với giá trị mặc định là CUSTOMER
ALTER TABLE notifications 
ADD COLUMN recipient VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER' 
AFTER link;

-- Tạo index cho cột recipient để tăng tốc độ query
CREATE INDEX idx_notification_recipient ON notifications(recipient);

-- Cập nhật comment cho bảng
ALTER TABLE notifications COMMENT = 'Bảng lưu trữ thông báo gửi đến customer hoặc manager';

-- Kiểm tra cấu trúc bảng sau khi thêm
DESCRIBE notifications;

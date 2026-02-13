-- Migration: Tạo notification_recipients cho các thông báo CUSTOMER đã tồn tại
-- Mục đích: Đồng bộ dữ liệu cũ để track được isRead status cho customer notifications

-- Tạo notification_recipients cho các thông báo CUSTOMER chưa có recipient record
INSERT INTO notification_recipients (id, notification_id, user_id, is_read, created_at, updated_at)
SELECT 
    UUID() as id,
    n.id as notification_id,
    n.user_id as user_id,
    FALSE as is_read,  -- Mặc định là chưa đọc
    n.created_at,
    n.updated_at
FROM notifications n
LEFT JOIN notification_recipients nr ON n.id = nr.notification_id AND n.user_id = nr.user_id
WHERE n.recipient = 'CUSTOMER'  -- Chỉ xử lý thông báo CUSTOMER
  AND nr.id IS NULL;  -- Chưa có bản ghi recipient

-- Kiểm tra kết quả
SELECT 
    COUNT(*) as total_customer_notifications,
    SUM(CASE WHEN nr.id IS NOT NULL THEN 1 ELSE 0 END) as notifications_with_recipients,
    SUM(CASE WHEN nr.id IS NULL THEN 1 ELSE 0 END) as notifications_without_recipients
FROM notifications n
LEFT JOIN notification_recipients nr ON n.id = nr.notification_id AND n.user_id = nr.user_id
WHERE n.recipient = 'CUSTOMER';

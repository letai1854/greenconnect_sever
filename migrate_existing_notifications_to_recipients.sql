-- ============================================================================
-- MIGRATE EXISTING NOTIFICATIONS TO NOTIFICATION_RECIPIENTS TABLE
-- ============================================================================
-- Mục đích: Tạo records trong notification_recipients cho các notifications 
--           hiện có để endpoint /notifications/manager hoạt động
-- ============================================================================

-- Bước 1: Kiểm tra trước khi migrate
SELECT 
    'notifications' as table_name,
    COUNT(*) as record_count
FROM notifications
WHERE recipient = 'MANAGER'

UNION ALL

SELECT 
    'notification_recipients' as table_name,
    COUNT(*) as record_count
FROM notification_recipients;

-- ============================================================================

-- Bước 2: Tạo notification_recipients cho TẤT CẢ managers
-- Mỗi notification MANAGER sẽ được gửi đến TẤT CẢ users có role ADMIN hoặc CUSTOMER_SUPPORT
INSERT INTO notification_recipients (id, notification_id, user_id, is_read, created_at)
SELECT 
    gen_random_uuid() as id,                    -- Tạo UUID mới
    n.id as notification_id,                     -- ID của notification
    u.id as user_id,                             -- ID của manager
    false as is_read,                            -- Mặc định chưa đọc
    n.created_at as created_at                   -- Giữ nguyên thời gian tạo
FROM notifications n
CROSS JOIN users u                               -- CROSS JOIN để tạo cho tất cả managers
WHERE n.recipient = 'MANAGER'                    -- Chỉ lấy notification MANAGER
  AND u.role IN ('ADMIN', 'CUSTOMER_SUPPORT')   -- Chỉ gửi cho ADMIN và CUSTOMER_SUPPORT
  AND NOT EXISTS (                               -- Tránh duplicate
      SELECT 1 
      FROM notification_recipients nr 
      WHERE nr.notification_id = n.id 
        AND nr.user_id = u.id
  );

-- ============================================================================

-- Bước 3: Kiểm tra kết quả sau khi migrate
SELECT 
    'notifications (MANAGER)' as description,
    COUNT(*) as count
FROM notifications
WHERE recipient = 'MANAGER'

UNION ALL

SELECT 
    'notification_recipients created' as description,
    COUNT(*) as count
FROM notification_recipients

UNION ALL

SELECT 
    'managers in system' as description,
    COUNT(*) as count
FROM users
WHERE role IN ('ADMIN', 'CUSTOMER_SUPPORT')

UNION ALL

SELECT 
    'expected recipients (notifications × managers)' as description,
    (SELECT COUNT(*) FROM notifications WHERE recipient = 'MANAGER') * 
    (SELECT COUNT(*) FROM users WHERE role IN ('ADMIN', 'CUSTOMER_SUPPORT')) as count;

-- ============================================================================

-- Bước 4: Kiểm tra chi tiết từng notification đã có bao nhiêu recipients
SELECT 
    n.id as notification_id,
    n.type,
    n.title,
    n.created_at,
    COUNT(nr.id) as recipient_count
FROM notifications n
LEFT JOIN notification_recipients nr ON n.id = nr.notification_id
WHERE n.recipient = 'MANAGER'
GROUP BY n.id, n.type, n.title, n.created_at
ORDER BY n.created_at DESC;

-- ============================================================================
-- LƯU Ý QUAN TRỌNG:
-- 1. Script này sẽ tạo notification_recipients cho TẤT CẢ managers hiện có
-- 2. Nếu có 5 notifications và 3 managers → tạo 5 × 3 = 15 records
-- 3. Tất cả đều có is_read = false (chưa đọc)
-- 4. Chỉ chạy script này 1 LẦN để tránh duplicate
-- 5. Nếu muốn chạy lại, phải xóa notification_recipients trước:
--    DELETE FROM notification_recipients;
-- ============================================================================

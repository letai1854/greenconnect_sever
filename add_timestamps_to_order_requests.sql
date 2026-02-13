-- Thêm cột created_at và updated_at nếu chưa có
USE greenconnect;

-- Kiểm tra và thêm created_at
ALTER TABLE order_requests 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Kiểm tra và thêm updated_at
ALTER TABLE order_requests 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Verify
DESCRIBE order_requests;

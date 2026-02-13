-- =====================================================
-- Migration Script: Add preferred_payment_method to users table
-- Description: Thêm cột preferred_payment_method cho phép user chọn phương thức thanh toán mặc định
-- Author: System
-- Date: 2025-11-12
-- =====================================================

USE greenconnect;

-- 1. Thêm cột preferred_payment_method vào bảng users
ALTER TABLE users 
ADD COLUMN preferred_payment_method VARCHAR(20) NOT NULL DEFAULT 'COD' 
COMMENT 'Phương thức thanh toán ưu tiên của user (COD, VNPAY, MOMO, VIETQR)';

-- 2. Thêm constraint để đảm bảo giá trị hợp lệ
ALTER TABLE users
ADD CONSTRAINT chk_preferred_payment_method 
CHECK (preferred_payment_method IN ('COD', 'VNPAY', 'MOMO', 'VIETQR'));

-- 3. Thêm index để tối ưu query theo payment method (nếu cần thống kê)
CREATE INDEX idx_users_payment_method ON users(preferred_payment_method);

-- 4. Cập nhật tất cả users hiện tại với giá trị mặc định COD (đã có DEFAULT nhưng để chắc chắn)
UPDATE users 
SET preferred_payment_method = 'COD' 
WHERE preferred_payment_method IS NULL OR preferred_payment_method = '';

-- 5. Verify migration
SELECT 
    COUNT(*) as total_users,
    preferred_payment_method,
    COUNT(*) * 100.0 / (SELECT COUNT(*) FROM users) as percentage
FROM users
GROUP BY preferred_payment_method
ORDER BY total_users DESC;

-- Expected result: Tất cả users sẽ có preferred_payment_method = 'COD'

-- =====================================================
-- ROLLBACK SCRIPT (Nếu cần revert)
-- =====================================================
-- DROP INDEX idx_users_payment_method ON users;
-- ALTER TABLE users DROP CONSTRAINT chk_preferred_payment_method;
-- ALTER TABLE users DROP COLUMN preferred_payment_method;
-- =====================================================

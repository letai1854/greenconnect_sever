-- Migration: Thêm bảng invitation_tokens và cập nhật PENDING_ACTIVATION status
-- Date: 2025-11-04
-- Description: Hệ thống mời user mới với activation token

-- 1. Thêm PENDING_ACTIVATION vào user status constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;
ALTER TABLE users ADD CONSTRAINT users_status_check 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_ACTIVATION'));

-- 2. Tạo bảng invitation_tokens
CREATE TABLE IF NOT EXISTS invitation_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(512) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    invited_by_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    activated_user_id UUID,
    
    -- Foreign keys
    CONSTRAINT fk_invitation_admin FOREIGN KEY (invited_by_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_activated_user FOREIGN KEY (activated_user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);

-- 3. Tạo indexes cho performance
CREATE INDEX idx_invitation_email ON invitation_tokens(email);
CREATE INDEX idx_invitation_token_hash ON invitation_tokens(token_hash);
CREATE INDEX idx_invitation_expiry ON invitation_tokens(expiry_date);
CREATE INDEX idx_invitation_status ON invitation_tokens(is_used, expiry_date);
CREATE INDEX idx_invitation_admin ON invitation_tokens(invited_by_id);

-- 4. Thêm comments cho documentation
COMMENT ON TABLE invitation_tokens IS 'Lưu activation tokens cho user mới được admin mời';
COMMENT ON COLUMN invitation_tokens.email IS 'Email của user được mời (có thể chưa tồn tại trong users table)';
COMMENT ON COLUMN invitation_tokens.full_name IS 'Họ tên user để gửi email';
COMMENT ON COLUMN invitation_tokens.token_hash IS 'SHA-256 hash của activation token';
COMMENT ON COLUMN invitation_tokens.expiry_date IS 'Thời gian hết hạn token (mặc định 24h)';
COMMENT ON COLUMN invitation_tokens.is_used IS 'Đánh dấu token đã được sử dụng';
COMMENT ON COLUMN invitation_tokens.invited_by_id IS 'UUID của admin tạo lời mời';
COMMENT ON COLUMN invitation_tokens.activated_at IS 'Thời gian user kích hoạt tài khoản';
COMMENT ON COLUMN invitation_tokens.activated_user_id IS 'UUID của user sau khi kích hoạt thành công';

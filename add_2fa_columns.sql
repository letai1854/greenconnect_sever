-- ============================================
-- Migration: Add TOTP 2FA fields to users table
-- Date: 2025-11-05
-- Description: Add secret_key_2fa and totp_enabled columns for 2FA authentication
-- ============================================

-- Add secret_key_2fa column (stores Base64 encoded secret key)
ALTER TABLE users 
ADD COLUMN secret_key_2fa VARCHAR(255) NULL
COMMENT 'Secret key cho TOTP 2FA (Base64 encoded)';

-- Add totp_enabled column (default FALSE for existing users)
ALTER TABLE users 
ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE
COMMENT 'Trạng thái kích hoạt 2FA (TRUE = đã kích hoạt, FALSE = chưa kích hoạt)';

-- Create index for faster lookup by totp_enabled status
CREATE INDEX idx_users_totp_enabled ON users(totp_enabled);

-- ============================================
-- Rollback Script (if needed)
-- ============================================
-- ALTER TABLE users DROP COLUMN secret_key_2fa;
-- ALTER TABLE users DROP COLUMN totp_enabled;
-- DROP INDEX idx_users_totp_enabled ON users;

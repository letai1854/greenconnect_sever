-- ========================================
-- Migration: Convert single role system to UserRole junction table
-- Version: V001
-- Description: Migrate existing user roles to UserRole table for many-to-many relationship
-- Date: September 2025
-- ========================================

-- Step 1: Create UserRole table if not exists
-- Note: This should already be created by JPA, but we include it for safety
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN', 'ORDER_MANAGER', 'PRODUCT_MANAGER', 'MARKETING_MANAGER', 'CUSTOMER_SUPPORT', 'SHIPPER') NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    -- Foreign key constraint
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint: one role per user (can be modified later if needed)
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role),
    
    -- Index for performance
    INDEX idx_user_roles_user_id (user_id),
    INDEX idx_user_roles_role (role),
    INDEX idx_user_roles_is_primary (is_primary),
    INDEX idx_user_roles_assigned_at (assigned_at),
    INDEX idx_user_roles_expires_at (expires_at)
);

-- Step 2: Migrate existing role data from users table to user_roles table
-- Only migrate users that have a role set and don't already have entries in user_roles
INSERT INTO user_roles (user_id, role, is_primary, assigned_at, created_at, updated_at)
SELECT 
    u.id,
    u.role,
    TRUE,  -- Mark as primary role
    u.created_at,  -- Use user creation time as assignment time
    NOW(6),
    NOW(6)
FROM users u
WHERE u.role IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur 
      WHERE ur.user_id = u.id AND ur.role = u.role
  );

-- Step 3: Verify migration - Check that all users with roles now have corresponding UserRole entries
-- This is a verification query - you can run it after migration to check
-- SELECT 
--     COUNT(*) as users_with_roles,
--     (SELECT COUNT(*) FROM user_roles WHERE is_primary = TRUE) as primary_role_assignments
-- FROM users 
-- WHERE role IS NOT NULL;

-- Step 4: Add constraints to ensure data integrity
-- Ensure each user has exactly one primary role
-- Note: This constraint might need to be added after initial migration and testing
-- ALTER TABLE user_roles 
-- ADD CONSTRAINT chk_one_primary_role_per_user 
-- CHECK (
--     (SELECT COUNT(*) FROM user_roles ur2 WHERE ur2.user_id = user_id AND ur2.is_primary = TRUE) <= 1
-- );

-- Step 5: Optional - Drop the old role column from users table
-- WARNING: Only uncomment this after thorough testing and verification
-- This is a destructive operation and should be done carefully

-- First, create a backup of the users table role data
CREATE TABLE IF NOT EXISTS users_role_backup AS
SELECT id, role, created_at, updated_at
FROM users
WHERE role IS NOT NULL;

-- Add comment about the backup
ALTER TABLE users_role_backup 
COMMENT = 'Backup of user roles before migration to UserRole system - Created during V001 migration';

-- Uncomment the following line only after thorough testing
-- ALTER TABLE users DROP COLUMN role;

-- Step 6: Update any existing users without roles to have CUSTOMER role
-- This ensures all users have at least one role assigned
INSERT INTO user_roles (user_id, role, is_primary, assigned_at, created_at, updated_at)
SELECT 
    u.id,
    'CUSTOMER',
    TRUE,
    NOW(6),
    NOW(6),
    NOW(6)
FROM users u
WHERE u.role IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id
  );

-- Step 7: Create indexes for optimal query performance
-- Additional indexes beyond those created in table creation
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id_primary ON user_roles (user_id, is_primary);
CREATE INDEX IF NOT EXISTS idx_user_roles_active ON user_roles (user_id, expires_at);

-- ========================================
-- Migration Verification Queries
-- ========================================
-- Run these queries after migration to verify success:

-- 1. Check all users have at least one role
-- SELECT 
--     u.id, 
--     u.email, 
--     u.role as old_role,
--     GROUP_CONCAT(ur.role) as new_roles,
--     SUM(ur.is_primary) as primary_count
-- FROM users u
-- LEFT JOIN user_roles ur ON u.id = ur.user_id
-- GROUP BY u.id, u.email, u.role
-- HAVING COUNT(ur.id) = 0 OR primary_count != 1;

-- 2. Verify role distribution
-- SELECT role, COUNT(*) as count FROM user_roles GROUP BY role ORDER BY count DESC;

-- 3. Check for users without primary roles
-- SELECT COUNT(*) as users_without_primary_role
-- FROM users u
-- WHERE NOT EXISTS (
--     SELECT 1 FROM user_roles ur 
--     WHERE ur.user_id = u.id AND ur.is_primary = TRUE
-- );

-- ========================================
-- Rollback Procedure (if needed)
-- ========================================
-- If rollback is needed, follow these steps:
-- 1. Restore role column in users table: ALTER TABLE users ADD COLUMN role ENUM(...);
-- 2. Restore role data: UPDATE users u SET role = (SELECT role FROM users_role_backup b WHERE b.id = u.id);
-- 3. Drop user_roles table: DROP TABLE user_roles;
-- 4. Drop backup table: DROP TABLE users_role_backup;
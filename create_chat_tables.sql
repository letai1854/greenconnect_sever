-- =============================================
-- Script: Tạo bảng Chat Session & Chat Message
-- Database: greenconnect-com
-- =============================================

-- 1️⃣ Tạo bảng chat_sessions (Phiên chat của user)
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key với bảng users
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Index để query nhanh
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at),
    INDEX idx_user_active (user_id, is_active, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2️⃣ Tạo bảng chat_messages (Tin nhắn trong từng session)
CREATE TABLE IF NOT EXISTS chat_messages (
    id BINARY(16) PRIMARY KEY,
    session_id BINARY(16) NOT NULL,
    sender ENUM('USER', 'BOT') NOT NULL,
    content TEXT NOT NULL,
    reference_product_ids TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key với bảng chat_sessions
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    
    -- Index để query nhanh
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- ✅ Xong! Chạy file này trong MySQL Workbench
-- =============================================

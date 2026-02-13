-- ================================================
-- SQL Script: Tạo bảng search_history và index
-- Mục đích: Lưu lịch sử tìm kiếm của customer
-- ================================================

-- Tạo bảng search_history
CREATE TABLE IF NOT EXISTS search_history (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    search_keyword VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    -- Foreign key
    CONSTRAINT fk_search_history_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử tìm kiếm của customer';

-- Tạo index cho user_id (tìm kiếm theo user)
CREATE INDEX idx_user_id ON search_history(user_id);

-- Tạo index cho created_at (sắp xếp theo thời gian)
CREATE INDEX idx_created_at ON search_history(created_at DESC);

-- Tạo composite index cho query hiệu quả (user_id + created_at)
CREATE INDEX idx_user_created ON search_history(user_id, created_at DESC);

-- ================================================
-- Kết thúc script
-- ================================================

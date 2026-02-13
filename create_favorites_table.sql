-- =====================================================
-- Migration: Create favorites table
-- Description: Bảng lưu trữ sản phẩm yêu thích của user
-- Giới hạn: Tối đa 20 sản phẩm yêu thích mỗi user
-- =====================================================

CREATE TABLE IF NOT EXISTS favorites (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    note TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    
    -- Unique constraint: Một user chỉ có thể yêu thích một product 1 lần
    CONSTRAINT uk_fav_user_product UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for performance
CREATE INDEX idx_favorite_user_active ON favorites(user_id, is_active);
CREATE INDEX idx_favorite_product ON favorites(product_id);
CREATE INDEX idx_favorite_created ON favorites(created_at);

-- Comments
ALTER TABLE favorites 
    COMMENT = 'Bảng lưu trữ sản phẩm yêu thích của user (tối đa 20 sản phẩm/user)';

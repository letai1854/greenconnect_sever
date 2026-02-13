-- Thêm cột discount_percentage vào bảng promotion_campaigns
-- File: add_discount_percentage_to_promotion_campaigns.sql
-- Date: 2025-11-08

ALTER TABLE promotion_campaigns 
ADD COLUMN discount_percentage DECIMAL(5, 2) 
COMMENT '% giảm giá chung cho campaign (0-100)';

-- Index cho query discount campaigns
CREATE INDEX idx_campaign_discount ON promotion_campaigns(discount_percentage) 
WHERE discount_percentage IS NOT NULL AND discount_percentage > 0;

-- Comments
COMMENT ON COLUMN promotion_campaigns.discount_percentage IS 'Phần trăm giảm giá chung áp dụng cho tất cả sản phẩm trong campaign (0-100). Nếu promotion_product có discount riêng, ưu tiên discount của product.';

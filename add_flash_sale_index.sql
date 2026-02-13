-- =====================================================
-- TỐI ƯU INDEX CHO FLASH SALE QUERY - MYSQL VERSION
-- =====================================================
-- Database: MySQL 8.0+
-- Mục đích: Tăng tốc query lấy sản phẩm trong campaign FLASH_SALE
-- Query tối ưu:
--   SELECT DISTINCT p FROM Product p
--   INNER JOIN PromotionProduct pp ON pp.product.id = p.id
--   INNER JOIN PromotionCampaign pc ON pp.campaign.id = pc.id
--   WHERE p.isActive = true
--     AND pc.isActive = true
--     AND pc.campaignType = 'FLASH_SALE'
--     AND pc.startDate <= NOW()
--     AND pc.endDate >= NOW()
--   ORDER BY pc.createdAt DESC, p.createdAt DESC

-- =====================================================
-- BƯỚC 1: KIỂM TRA INDEX HIỆN CÓ
-- =====================================================
-- Xem các index đang tồn tại trên bảng promotion_campaigns
SHOW INDEXES FROM promotion_campaigns;

-- =====================================================
-- BƯỚC 2: XÓA INDEX CŨ (NẾU TỒN TẠI)
-- =====================================================
-- Kiểm tra và xóa index cũ nếu đã tồn tại
DROP INDEX IF EXISTS idx_campaign_flash_sale ON promotion_campaigns;

-- =====================================================
-- BƯỚC 3: TẠO COMPOSITE INDEX MỚI
-- =====================================================
-- Covering index bao gồm tất cả columns trong WHERE + ORDER BY
-- Thứ tự: campaign_type (high selectivity) -> is_active -> dates -> created_at
-- 
-- ⚠️ LƯU Ý: MySQL không hỗ trợ DESC trong index definition như PostgreSQL
-- Nhưng optimizer vẫn có thể sử dụng index cho ORDER BY DESC
CREATE INDEX idx_campaign_flash_sale 
ON promotion_campaigns (
    campaign_type,      -- ⭐ High selectivity (FLASH_SALE, SEASONAL, ...)
    is_active,          -- Boolean filter (TINYINT(1) trong MySQL)
    start_date,         -- Range filter (<=)
    end_date,           -- Range filter (>=)
    created_at          -- ORDER BY optimization (MySQL sẽ tự động reverse scan cho DESC)
);

-- =====================================================
-- PHÂN TÍCH HIỆU QUẢ:
-- =====================================================
-- ✅ BEFORE: Full table scan trên promotion_campaigns (slow)
-- ✅ AFTER: Index seek với campaign_type -> filter dates -> sort by created_at
--
-- Ước tính cải thiện:
-- - 10,000 campaigns -> Filter down to ~100 FLASH_SALE -> ~10 active & valid
-- - Query time: ~500ms -> ~10ms (50x faster)
--
-- Trade-off:
-- - Disk space: ~2-3 KB per 1000 campaigns (negligible)
-- - Insert/Update overhead: Minimal (<5ms)

-- =====================================================
-- BƯỚC 4: VERIFY INDEX
-- =====================================================
-- Kiểm tra index đã tạo thành công:
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    COLLATION,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'promotion_campaigns'
  AND INDEX_NAME = 'idx_campaign_flash_sale'
ORDER BY SEQ_IN_INDEX;

-- =====================================================
-- BƯỚC 5: TEST QUERY PERFORMANCE
-- =====================================================
-- Kiểm tra query plan để đảm bảo index được sử dụng
EXPLAIN
SELECT DISTINCT p.* 
FROM products p
INNER JOIN promotion_products pp ON pp.product_id = p.id
INNER JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
WHERE p.is_active = true
  AND pc.is_active = true
  AND pc.campaign_type = 'FLASH_SALE'
  AND pc.start_date <= NOW()
  AND pc.end_date >= NOW()
ORDER BY pc.created_at DESC, p.created_at DESC
LIMIT 20;

-- Expected: "type" column should show "ref" or "range" (not "ALL")
-- Expected: "key" column should show "idx_campaign_flash_sale"

-- =====================================================
-- BƯỚC 6 (OPTIONAL): ANALYZE TABLE
-- =====================================================
-- Cập nhật statistics để MySQL optimizer sử dụng index hiệu quả
ANALYZE TABLE promotion_campaigns;
ANALYZE TABLE promotion_products;
ANALYZE TABLE products;

-- =====================================================
-- ROLLBACK (NẾU CẦN)
-- =====================================================
-- Nếu muốn xóa index này:
-- DROP INDEX idx_campaign_flash_sale ON promotion_campaigns;

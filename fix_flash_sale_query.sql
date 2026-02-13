-- -- =====================================================
-- -- FIXED FLASH SALE QUERY - MYSQL VERSION
-- -- =====================================================
-- -- Fix: MySQL DISTINCT + ORDER BY incompatibility
-- -- Problem: ORDER BY columns must be in SELECT when using DISTINCT
-- -- Solution: Use subquery to separate logic

-- -- =====================================================
-- -- BƯỚC 1: TEST QUERY (Subquery approach)
-- -- =====================================================

-- -- Fixed query - Using subquery
-- SELECT DISTINCT p.* 
-- FROM products p
-- WHERE p.id IN (
--   SELECT DISTINCT pp.product_id 
--   FROM promotion_products pp
--   INNER JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
--   WHERE p.is_active = true
--     AND pc.is_active = true
--     AND pc.campaign_type = 'FLASH_SALE'
--     AND pc.start_date <= NOW()
--     AND pc.end_date >= NOW()
-- )
-- ORDER BY p.created_at DESC
-- LIMIT 20;

-- -- =====================================================
-- -- BƯỚC 2: VERIFY INDEX USAGE
-- -- =====================================================

-- -- Check if index is being used
-- EXPLAIN
-- SELECT DISTINCT p.* 
-- FROM products p
-- WHERE p.id IN (
--   SELECT DISTINCT pp.product_id 
--   FROM promotion_products pp
--   INNER JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
--   WHERE p.is_active = true
--     AND pc.is_active = true
--     AND pc.campaign_type = 'FLASH_SALE'
--     AND pc.start_date <= NOW()
--     AND pc.end_date >= NOW()
-- )
-- ORDER BY p.created_at DESC
-- LIMIT 20;

-- -- Expected output:
-- -- - Subquery should use: idx_campaign_flash_sale
-- -- - Outer query should use: idx_product_active_created or PRIMARY KEY

-- -- =====================================================
-- -- BƯỚC 3: PERFORMANCE TEST
-- -- =====================================================

-- -- Count products in FLASH_SALE campaigns
-- SELECT COUNT(DISTINCT pp.product_id) AS flash_sale_product_count
-- FROM promotion_products pp
-- INNER JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
-- WHERE pc.is_active = true
--   AND pc.campaign_type = 'FLASH_SALE'
--   AND pc.start_date <= NOW()
--   AND pc.end_date >= NOW();

-- -- =====================================================
-- -- BƯỚC 4: ANALYZE TABLE STATISTICS
-- -- =====================================================

-- -- Update statistics for optimizer
-- ANALYZE TABLE products;
-- ANALYZE TABLE promotion_products;
-- ANALYZE TABLE promotion_campaigns;

-- -- =====================================================
-- -- BƯỚC 5: SUMMARY OF CHANGES
-- -- =====================================================

-- -- BEFORE (❌ LỖI):
-- -- SELECT DISTINCT p.*
-- -- FROM products p
-- -- JOIN promotion_products pp ON pp.product_id = p.id
-- -- JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
-- -- WHERE ...
-- -- ORDER BY pc.created_at DESC;  -- ❌ MySQL error: DISTINCT + ORDER BY

-- -- AFTER (✅ FIXED):
-- -- SELECT p.*
-- -- FROM products p
-- -- WHERE p.id IN (
-- --   SELECT DISTINCT pp.product_id 
-- --   FROM promotion_products pp
-- --   JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
-- --   WHERE ...
-- -- )
-- -- ORDER BY p.created_at DESC;  -- ✅ MySQL compatible

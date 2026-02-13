-- -- =====================================================
-- -- KIỂM TRA CẤU TRÚC DATABASE
-- -- =====================================================
-- -- Chạy script này để xem tên cột thực tế trong database

-- -- 1. Kiểm tra cấu trúc bảng promotion_campaigns
-- DESCRIBE promotion_campaigns;

-- -- 2. Kiểm tra indexes hiện có
-- SHOW INDEXES FROM promotion_campaigns;

-- -- 3. Kiểm tra cấu trúc bảng products
-- DESCRIBE products;

-- -- 4. Kiểm tra indexes hiện có
-- SHOW INDEXES FROM products;

-- -- 5. Kiểm tra cấu trúc bảng promotion_products
-- DESCRIBE promotion_products;

-- -- 6. Kiểm tra indexes hiện có
-- SHOW INDEXES FROM promotion_products;

-- -- 7. Test query hiện tại (để xem có lỗi không)
-- EXPLAIN
-- SELECT DISTINCT p.* 
-- FROM products p
-- INNER JOIN promotion_products pp ON pp.product_id = p.id
-- INNER JOIN promotion_campaigns pc ON pp.campaign_id = pc.id
-- WHERE p.is_active = true
--   AND pc.is_active = true
--   AND pc.campaign_type = 'FLASH_SALE'
--   AND pc.start_date <= NOW()
--   AND pc.end_date >= NOW()
-- ORDER BY pc.created_at DESC, p.created_at DESC
-- LIMIT 20;

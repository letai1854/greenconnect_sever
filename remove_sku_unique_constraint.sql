-- -- ===================================================================
-- -- XÓA UNIQUE CONSTRAINT TRÊN CỘT SKU
-- -- ===================================================================
-- -- Constraint name: UKq935p2d1pbjm39n0063ghnfgn
-- -- Lý do: Cho phép nhiều variants dùng chung SKU
-- -- File đã sửa: ProductVariant.java (bỏ unique=true)
-- -- ===================================================================

-- USE greenconnect;

-- -- 1. Kiểm tra constraint hiện tại
-- SELECT 
--     CONSTRAINT_NAME,
--     CONSTRAINT_TYPE,
--     TABLE_NAME
-- FROM information_schema.TABLE_CONSTRAINTS
-- WHERE TABLE_SCHEMA = 'greenconnect' 
--   AND TABLE_NAME = 'product_variants'
--   AND CONSTRAINT_TYPE = 'UNIQUE';

-- -- 2. Xóa UNIQUE constraint trên SKU (nếu tồn tại)
-- ALTER TABLE product_variants 
-- DROP INDEX IF EXISTS UKq935p2d1pbjm39n0063ghnfgn;

-- -- Alternative (nếu constraint có tên khác):
-- -- ALTER TABLE product_variants DROP INDEX sku;
-- -- ALTER TABLE product_variants DROP KEY sku;

-- -- 3. (Optional) Thêm index thường (không unique) để tăng tốc query
-- -- Chỉ tạo nếu chưa có
-- CREATE INDEX IF NOT EXISTS idx_variant_sku ON product_variants(sku);

-- -- 4. Verify constraint đã bị xóa
-- SELECT 
--     CONSTRAINT_NAME,
--     CONSTRAINT_TYPE,
--     TABLE_NAME
-- FROM information_schema.TABLE_CONSTRAINTS
-- WHERE TABLE_SCHEMA = 'greenconnect' 
--   AND TABLE_NAME = 'product_variants'
--   AND CONSTRAINT_TYPE = 'UNIQUE';

-- -- 5. Kiểm tra các index hiện có
-- SHOW INDEX FROM product_variants WHERE Key_name LIKE '%sku%';

-- -- ===================================================================
-- -- HƯỚNG DẪN:
-- -- 1. Chạy script này trong MySQL
-- -- 2. Hoặc restart Spring Boot app (Hibernate sẽ tự drop constraint)
-- -- 3. Verify bằng cách insert 2 variants cùng SKU
-- -- ===================================================================

-- -- ===================================================================
-- -- KẾT QUẢ MONG ĐỢI:
-- -- - UNIQUE constraint đã bị xóa
-- -- - Index thường (idx_variant_sku) đã được tạo (optional)
-- -- - Có thể insert nhiều variants với cùng SKU
-- -- ===================================================================

-- -- ===================================================================
-- -- TEST QUERY (sau khi xóa constraint):
-- -- ===================================================================
-- -- INSERT INTO product_variants (id, product_id, name, sku, price, stock_quantity, unit, is_active, is_default)
-- -- VALUES 
-- --   (UUID(), 'product-uuid-1', 'Variant 1', 'DUPLICATE-SKU', 100000, 10, 'kg', 1, 0),
-- --   (UUID(), 'product-uuid-1', 'Variant 2', 'DUPLICATE-SKU', 200000, 20, 'kg', 1, 0);
-- -- 
-- -- Expected: Success (không còn lỗi duplicate)

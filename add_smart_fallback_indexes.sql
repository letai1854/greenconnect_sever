-- =============================================
-- Smart Fallback Strategy - Performance Indexes
-- =============================================

-- 1️⃣ Index cho NEW products (ORDER BY created_at DESC)
CREATE INDEX IF NOT EXISTS idx_products_new 
ON products(is_active, created_at DESC)
WHERE is_active = true;

-- 2️⃣ Index cho TRENDING products (ORDER BY sell_number DESC)
CREATE INDEX IF NOT EXISTS idx_products_trending 
ON products(is_active, sell_number DESC, created_at DESC)
WHERE is_active = true;

-- 3️⃣ Index cho TOP RATED products (ORDER BY average_rating DESC)
CREATE INDEX IF NOT EXISTS idx_products_rated 
ON products(is_active, average_rating DESC, review_count DESC)
WHERE is_active = true AND review_count >= 5;

-- 4️⃣ Index cho FEATURED products (is_featured = true)
CREATE INDEX IF NOT EXISTS idx_products_featured 
ON products(is_active, is_featured, sell_number DESC)
WHERE is_active = true AND is_featured = true;

-- =============================================
-- Verify indexes
-- =============================================
SHOW INDEXES FROM products WHERE Key_name LIKE 'idx_products_%';

-- =============================================
-- Test query performance
-- =============================================

-- Test NEW products query
EXPLAIN SELECT p.* FROM products p
WHERE p.is_active = true
ORDER BY p.created_at DESC
LIMIT 30;

-- Test TRENDING products query
EXPLAIN SELECT p.* FROM products p
WHERE p.is_active = true
ORDER BY p.sell_number DESC, p.created_at DESC
LIMIT 30;

-- Test TOP RATED products query
EXPLAIN SELECT p.* FROM products p
WHERE p.is_active = true AND p.review_count >= 5
ORDER BY p.average_rating DESC, p.review_count DESC
LIMIT 30;

-- Test FEATURED products query
EXPLAIN SELECT p.* FROM products p
WHERE p.is_active = true AND p.is_featured = true
ORDER BY p.sell_number DESC, p.created_at DESC
LIMIT 30;

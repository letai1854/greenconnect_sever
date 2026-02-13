-- ============================================================
-- Migration Script: Add Review & Product Indexes for Top-Rated Query
-- Date: 2025-11-12
-- Purpose: Optimize queries for product reviews and media lookup
-- ============================================================

-- ⭐ ProductReview Indexes (2 NEW)
-- These indexes enable efficient sorting by rating and filtering approved reviews

-- Index 1: For finding top-rated reviews per product with time sorting
-- Query Pattern: SELECT reviews WHERE product=X ORDER BY rating DESC, reviewTime DESC LIMIT 1
CREATE INDEX IF NOT EXISTS idx_review_product_rating_time 
ON product_reviews(product_id, rating DESC, review_time DESC);

-- Index 2: For filtering approved reviews sorted by rating
-- Query Pattern: SELECT reviews WHERE product=X AND approved=1 ORDER BY rating DESC
CREATE INDEX IF NOT EXISTS idx_review_product_approved_rating 
ON product_reviews(product_id, is_approved, rating DESC);

-- ⭐ ReviewMedia Index (1 NEW)
-- Enables efficient retrieval of media by display order

-- Index 3: For fetching media sorted by display order
-- Query Pattern: SELECT media WHERE review=X ORDER BY displayOrder ASC LIMIT 1
CREATE INDEX IF NOT EXISTS idx_review_media_display_order 
ON media_reviews(review_id, display_order ASC);

-- ============================================================
-- Verification Queries
-- ============================================================
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS 
-- WHERE TABLE_NAME = 'product_reviews' AND INDEX_NAME LIKE 'idx_review%';
--
-- SELECT * FROM INFORMATION_SCHEMA.STATISTICS 
-- WHERE TABLE_NAME = 'media_reviews' AND INDEX_NAME LIKE 'idx_review%';
-- ============================================================

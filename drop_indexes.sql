-- -- =====================================
-- -- SCRIPT DROP INDEX CHO GREENCONNECT DATABASE
-- -- Sử dụng khi cần gỡ bỏ các index đã tạo
-- -- =====================================

-- -- =====================================
-- -- 1. DROP PRODUCTS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_products_slug;
-- DROP INDEX IF EXISTS idx_products_category_id;
-- DROP INDEX IF EXISTS idx_products_supplier_id;
-- DROP INDEX IF EXISTS idx_products_active_featured;
-- DROP INDEX IF EXISTS idx_products_rating_created;
-- DROP INDEX IF EXISTS idx_products_active_created;
-- DROP INDEX IF EXISTS idx_products_category_active_rating;
-- DROP INDEX IF EXISTS idx_products_supplier_active_rating;
-- DROP INDEX IF EXISTS idx_products_name;

-- -- =====================================
-- -- 2. DROP ORDERS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_orders_order_code;
-- DROP INDEX IF EXISTS idx_orders_user_created;
-- DROP INDEX IF EXISTS idx_orders_status_date;
-- DROP INDEX IF EXISTS idx_orders_payment_status_date;
-- DROP INDEX IF EXISTS idx_orders_date_status;
-- DROP INDEX IF EXISTS idx_orders_id_user;

-- -- =====================================
-- -- 3. DROP CART_ITEMS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_cart_items_user_added;
-- DROP INDEX IF EXISTS idx_cart_items_guest_added;
-- DROP INDEX IF EXISTS idx_cart_items_user_variant;
-- DROP INDEX IF EXISTS idx_cart_items_guest_variant;
-- DROP INDEX IF EXISTS idx_cart_items_variant;

-- -- =====================================
-- -- 4. DROP PRODUCT_REVIEWS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_reviews_product_time;
-- DROP INDEX IF EXISTS idx_reviews_user_order_detail;
-- DROP INDEX IF EXISTS idx_reviews_approved_time;
-- DROP INDEX IF EXISTS idx_reviews_product_rating;

-- -- =====================================
-- -- 5. DROP USERS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_users_email;
-- DROP INDEX IF EXISTS idx_users_role_status;
-- DROP INDEX IF EXISTS idx_users_fullname;
-- DROP INDEX IF EXISTS idx_users_status_created;

-- -- =====================================
-- -- 6. DROP PRODUCT_VARIANTS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_variants_product_active;
-- DROP INDEX IF EXISTS idx_variants_sku;
-- DROP INDEX IF EXISTS idx_variants_product_default;
-- DROP INDEX IF EXISTS idx_variants_price;
-- DROP INDEX IF EXISTS idx_variants_stock;

-- -- =====================================
-- -- 7. DROP CATEGORIES TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_categories_slug;
-- DROP INDEX IF EXISTS idx_categories_active;

-- -- =====================================
-- -- 8. DROP SUPPLIERS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_suppliers_name;
-- DROP INDEX IF EXISTS idx_suppliers_featured;

-- -- =====================================
-- -- 9. DROP ORDER_DETAILS TABLE INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_order_details_order_id;
-- DROP INDEX IF EXISTS idx_order_details_variant;

-- -- =====================================
-- -- 10. DROP ADDITIONAL INDEXES
-- -- =====================================
-- DROP INDEX IF EXISTS idx_vouchers_code;
-- DROP INDEX IF EXISTS idx_vouchers_active_dates;
-- DROP INDEX IF EXISTS idx_refresh_tokens_token;
-- DROP INDEX IF EXISTS idx_refresh_tokens_user;
-- DROP INDEX IF EXISTS idx_product_images_product;
-- DROP INDEX IF EXISTS idx_banners_active_position;
-- DROP INDEX IF EXISTS idx_addresses_user;
-- DROP INDEX IF EXISTS idx_order_status_history_order;
-- DROP INDEX IF EXISTS idx_order_status_history_time;
-- DROP INDEX IF EXISTS idx_conversations_participants;
-- DROP INDEX IF EXISTS idx_messages_conversation_time;
-- DROP INDEX IF EXISTS idx_supplier_media_supplier;
-- DROP INDEX IF EXISTS idx_review_media_review;
-- DROP INDEX IF EXISTS idx_promotion_products_campaign;
-- DROP INDEX IF EXISTS idx_promotion_products_product;

-- -- =====================================
-- -- THÔNG BÁO HOÀN THÀNH
-- -- =====================================
-- -- Tất cả các index đã được gỡ bỏ thành công!
-- -- =====================================
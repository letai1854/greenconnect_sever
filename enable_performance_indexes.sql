-- -- =====================================
-- -- ENABLE PERFORMANCE INDEXES
-- -- Script để bật các index quan trọng cho hiệu suất
-- -- =====================================

-- -- =====================================
-- -- 1. PRODUCT IMAGES INDEXES (MỚI - QUAN TRỌNG)
-- -- =====================================

-- -- Index cho lấy images theo product (dùng trong ProductMapper, CartItemRepository)
-- CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);

-- -- Index cho lấy ảnh chính theo product
-- CREATE INDEX IF NOT EXISTS idx_product_images_product_main ON product_images(product_id, is_main);

-- -- Index cho sắp xếp theo display_order
-- CREATE INDEX IF NOT EXISTS idx_product_images_product_order ON product_images(product_id, display_order);

-- -- =====================================
-- -- 2. PRODUCT VARIANTS INDEXES
-- -- =====================================

-- -- Index cho lấy variants theo product với filter active
-- CREATE INDEX IF NOT EXISTS idx_variants_product_active ON product_variants(product_id, is_active);

-- -- Index cho lấy variant mặc định của product
-- CREATE INDEX IF NOT EXISTS idx_variants_product_default ON product_variants(product_id, is_default);

-- -- Index cho tìm kiếm theo SKU (unique)
-- CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku);

-- -- Index cho stock quantity (inventory management)
-- CREATE INDEX IF NOT EXISTS idx_variants_stock ON product_variants(stock_quantity);

-- -- Index cho price range filtering
-- CREATE INDEX IF NOT EXISTS idx_variants_price ON product_variants(price);

-- -- =====================================
-- -- 3. CART ITEMS INDEXES
-- -- =====================================

-- -- Index cho cart của user với thời gian thêm
-- CREATE INDEX IF NOT EXISTS idx_cart_items_user_added ON cart_items(user_id, added_date DESC);

-- -- Index cho kiểm tra duplicate item trong cart
-- CREATE INDEX IF NOT EXISTS idx_cart_items_user_variant ON cart_items(user_id, productvariant_id);

-- -- Index cho quản lý stock theo variant
-- CREATE INDEX IF NOT EXISTS idx_cart_items_variant ON cart_items(productvariant_id);

-- -- Index cho đếm số lượng items trong cart
-- CREATE INDEX IF NOT EXISTS idx_cart_user_count ON cart_items(user_id);

-- -- Index cho check ownership
-- CREATE INDEX IF NOT EXISTS idx_cart_id_user ON cart_items(id, user_id);

-- -- =====================================
-- -- 4. PRODUCTS TABLE INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo slug (URL-friendly)
-- CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);

-- -- Index cho filter theo category
-- CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);

-- -- Index cho filter theo supplier
-- CREATE INDEX IF NOT EXISTS idx_products_supplier_id ON products(supplier_id);

-- -- Index cho lọc sản phẩm active và featured
-- CREATE INDEX IF NOT EXISTS idx_products_active_featured ON products(is_active, is_featured);

-- -- Index cho sắp xếp theo rating và thời gian tạo
-- CREATE INDEX IF NOT EXISTS idx_products_rating_created ON products(average_rating DESC, created_at DESC);

-- -- Index cho sắp xếp sản phẩm active theo thời gian
-- CREATE INDEX IF NOT EXISTS idx_products_active_created ON products(is_active, created_at DESC);

-- -- =====================================
-- -- 5. ORDERS TABLE INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo order code
-- CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);

-- -- Index cho lấy đơn hàng của user theo thời gian
-- CREATE INDEX IF NOT EXISTS idx_orders_user_created ON orders(user_id, order_date DESC);

-- -- Index cho admin filter đơn hàng theo status và thời gian
-- CREATE INDEX IF NOT EXISTS idx_orders_status_date ON orders(order_status, order_date DESC);

-- -- Index cho filter theo payment status
-- CREATE INDEX IF NOT EXISTS idx_orders_payment_status_date ON orders(payment_status, order_date DESC);

-- -- Index cho check ownership
-- CREATE INDEX IF NOT EXISTS idx_orders_id_user ON orders(id, user_id);

-- -- =====================================
-- -- 6. ORDER DETAILS INDEXES
-- -- =====================================

-- -- Index cho lấy chi tiết theo order
-- CREATE INDEX IF NOT EXISTS idx_order_details_order_id ON order_details(order_id);

-- -- Index cho thống kê theo variant (báo cáo sản phẩm bán chạy)
-- CREATE INDEX IF NOT EXISTS idx_order_details_variant ON order_details(variant_id);

-- -- =====================================
-- -- 7. ADDRESSES INDEXES
-- -- =====================================

-- -- Index cho lấy địa chỉ của user
-- CREATE INDEX IF NOT EXISTS idx_addresses_user ON addresses(user_id);

-- -- Index cho lấy địa chỉ mặc định của user
-- CREATE INDEX IF NOT EXISTS idx_address_user_default ON addresses(user_id, is_default);

-- -- =====================================
-- -- 8. CATEGORIES INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo slug
-- CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);

-- -- Index cho filter active categories
-- CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active);

-- -- Index cho sắp xếp theo display order
-- CREATE INDEX IF NOT EXISTS idx_category_display_order ON categories(display_order);

-- -- =====================================
-- -- 9. PRODUCT REVIEWS INDEXES
-- -- =====================================

-- -- Index cho lấy reviews theo sản phẩm với sắp xếp theo thời gian
-- CREATE INDEX IF NOT EXISTS idx_reviews_product_time ON product_reviews(product_id, review_time DESC);

-- -- Index cho kiểm tra duplicate review
-- CREATE INDEX IF NOT EXISTS idx_reviews_user_order_detail ON product_reviews(user_id, order_detail_id);

-- -- Index cho admin kiểm duyệt reviews
-- CREATE INDEX IF NOT EXISTS idx_reviews_approved_time ON product_reviews(is_approved, review_time DESC);

-- -- =====================================
-- -- 10. USERS TABLE INDEXES
-- -- =====================================

-- -- Index cho login bằng email
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- -- Index cho filter user theo role và status
-- CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(role, status);

-- -- =====================================
-- -- 11. VOUCHERS INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo code
-- CREATE INDEX IF NOT EXISTS idx_vouchers_code ON vouchers(code);

-- -- Index cho filter vouchers active theo dates
-- CREATE INDEX IF NOT EXISTS idx_vouchers_active_dates ON vouchers(is_active, start_date, end_date);

-- -- =====================================
-- -- 12. BANNERS INDEXES
-- -- =====================================

-- -- Index cho filter banner active theo group và order
-- CREATE INDEX IF NOT EXISTS idx_banner_group_active_order ON banners(banner_group_id, is_active, display_order);

-- -- =====================================
-- -- KIỂM TRA KẾT QUẢ
-- -- =====================================

-- -- Liệt kê tất cả indexes của các bảng quan trọng
-- SELECT 
--     tablename,
--     indexname,
--     indexdef
-- FROM pg_indexes
-- WHERE tablename IN ('products', 'product_variants', 'product_images', 'cart_items', 'orders', 'order_details')
-- ORDER BY tablename, indexname;

-- -- =====================================
-- -- Script hoàn thành!
-- -- Tất cả indexes đã được tạo để tối ưu hiệu suất
-- -- =====================================

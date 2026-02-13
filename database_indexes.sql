-- -- =====================================
-- -- GREENCONNECT DATABASE INDEX OPTIMIZATION
-- -- Script để thêm các index cần thiết cho tối ưu hóa hiệu suất
-- -- =====================================

-- -- =====================================
-- -- 1. PRODUCTS TABLE INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm sản phẩm theo slug (URL-friendly)
-- -- Sử dụng trong: ProductRepository.findBySlug()
-- CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);

-- -- Index cho tìm kiếm theo category 
-- -- Sử dụng trong: JpaSpecificationExecutor để filter theo danh mục
-- CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);

-- -- Index cho tìm kiếm theo supplier
-- -- Sử dụng trong: JpaSpecificationExecutor để filter theo nhà cung cấp  
-- CREATE INDEX IF NOT EXISTS idx_products_supplier_id ON products(supplier_id);

-- -- Index cho lọc sản phẩm active và featured
-- -- Sử dụng trong: Filter sản phẩm hoạt động và nổi bật
-- CREATE INDEX IF NOT EXISTS idx_products_active_featured ON products(is_active, is_featured);

-- -- Index cho sắp xếp theo rating và thời gian tạo
-- -- Sử dụng trong: Sắp xếp sản phẩm theo đánh giá cao nhất
-- CREATE INDEX IF NOT EXISTS idx_products_rating_created ON products(average_rating DESC, created_at DESC);

-- -- Index cho sắp xếp sản phẩm active theo thời gian
-- -- Sử dụng trong: Hiển thị sản phẩm mới nhất
-- CREATE INDEX IF NOT EXISTS idx_products_active_created ON products(is_active, created_at DESC);

-- -- Composite index cho truy vấn phức tạp theo category + active + rating
-- -- Sử dụng trong: Trang danh mục với sắp xếp theo rating
-- CREATE INDEX IF NOT EXISTS idx_products_category_active_rating ON products(category_id, is_active, average_rating DESC);

-- -- Composite index cho truy vấn phức tạp theo supplier + active + rating  
-- -- Sử dụng trong: Trang nhà cung cấp với sắp xếp theo rating
-- CREATE INDEX IF NOT EXISTS idx_products_supplier_active_rating ON products(supplier_id, is_active, average_rating DESC);

-- -- Index cho tìm kiếm full-text theo tên sản phẩm
-- -- Sử dụng trong: ProductRepository.findByNameContainingIgnoreCase()
-- CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);

-- -- =====================================  
-- -- 2. ORDERS TABLE INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo order code
-- -- Sử dụng trong: OrderRepository.findByCode()
-- CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);

-- -- Index cho lấy đơn hàng của user theo thời gian
-- -- Sử dụng trong: OrderRepository.findByUserIdOrderByCreatedAtDesc()
-- CREATE INDEX IF NOT EXISTS idx_orders_user_created ON orders(user_id, order_date DESC);

-- -- Index cho admin filter đơn hàng theo status và thời gian
-- -- Sử dụng trong: JpaSpecificationExecutor để filter theo trạng thái
-- CREATE INDEX IF NOT EXISTS idx_orders_status_date ON orders(order_status, order_date DESC);

-- -- Index cho filter theo payment status
-- -- Sử dụng trong: Admin filter đơn hàng theo trạng thái thanh toán
-- CREATE INDEX IF NOT EXISTS idx_orders_payment_status_date ON orders(payment_status, order_date DESC);

-- -- Index cho thống kê theo thời gian
-- -- Sử dụng trong: Báo cáo doanh thu theo ngày/tháng
-- CREATE INDEX IF NOT EXISTS idx_orders_date_status ON orders(order_date, order_status);

-- -- Index cho tìm theo user và order
-- -- Sử dụng trong: OrderRepository.findByIdAndUserId()  
-- CREATE INDEX IF NOT EXISTS idx_orders_id_user ON orders(id, user_id);

-- -- =====================================
-- -- 3. CART_ITEMS TABLE INDEXES  
-- -- =====================================

-- -- Index cho cart của user đã đăng nhập
-- -- Sử dụng trong: CartItemRepository.findByUserOrderByAddedDateDesc()
-- CREATE INDEX IF NOT EXISTS idx_cart_items_user_added ON cart_items(user_id, added_date DESC);

-- -- Index cho cart của guest user
-- -- Sử dụng trong: CartItemRepository.findByGuestIdOrderByAddedDateDesc()
-- CREATE INDEX IF NOT EXISTS idx_cart_items_guest_added ON cart_items(guest_id, added_date DESC);

-- -- Index cho kiểm tra duplicate item trong cart của user
-- -- Sử dụng trong: CartItemRepository.findByUserAndProductVariant()
-- CREATE INDEX IF NOT EXISTS idx_cart_items_user_variant ON cart_items(user_id, product_variant_id);

-- -- Index cho kiểm tra duplicate item trong cart của guest
-- -- Sử dụng trong: CartItemRepository.findByGuestIdAndProductVariant()
-- CREATE INDEX IF NOT EXISTS idx_cart_items_guest_variant ON cart_items(guest_id, product_variant_id);

-- -- Index cho quản lý stock theo variant
-- -- Sử dụng trong: CartItemRepository.findByProductVariant()
-- CREATE INDEX IF NOT EXISTS idx_cart_items_variant ON cart_items(product_variant_id);

-- -- =====================================
-- -- 4. PRODUCT_REVIEWS TABLE INDEXES
-- -- =====================================

-- -- Index cho lấy reviews theo sản phẩm với sắp xếp theo thời gian
-- -- Sử dụng trong: ProductReviewRepository.findByProductId()
-- CREATE INDEX IF NOT EXISTS idx_reviews_product_time ON product_reviews(product_id, review_time DESC);

-- -- Index cho kiểm tra duplicate review 
-- -- Sử dụng trong: ProductReviewRepository.existsByUserIdAndOrderDetailId()
-- CREATE INDEX IF NOT EXISTS idx_reviews_user_order_detail ON product_reviews(user_id, order_detail_id);

-- -- Index cho admin kiểm duyệt reviews
-- -- Sử dụng trong: JpaSpecificationExecutor để filter theo trạng thái duyệt
-- CREATE INDEX IF NOT EXISTS idx_reviews_approved_time ON product_reviews(is_approved, review_time DESC);

-- -- Index cho thống kê rating theo sản phẩm
-- -- Sử dụng trong: Tính toán average rating và review count
-- CREATE INDEX IF NOT EXISTS idx_reviews_product_rating ON product_reviews(product_id, rating);

-- -- =====================================
-- -- 5. USERS TABLE INDEXES
-- -- =====================================

-- -- Index cho login bằng email
-- -- Sử dụng trong: UserRepository.findByEmailIgnoreCase()
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- -- Index cho filter user theo role và status
-- -- Sử dụng trong: JpaSpecificationExecutor để filter user
-- CREATE INDEX IF NOT EXISTS idx_users_role_status ON users(role, status);

-- -- Index cho search user theo tên
-- -- Sử dụng trong: JpaSpecificationExecutor để tìm kiếm user
-- CREATE INDEX IF NOT EXISTS idx_users_fullname ON users(full_name);

-- -- Index cho thống kê user active theo thời gian
-- -- Sử dụng trong: Báo cáo user đăng ký mới
-- CREATE INDEX IF NOT EXISTS idx_users_status_created ON users(status, created_at DESC);

-- -- =====================================
-- -- 6. PRODUCT_VARIANTS TABLE INDEXES
-- -- =====================================

-- -- Index cho lấy variants theo product
-- -- Sử dụng trong: Hiển thị các biến thể của sản phẩm
-- CREATE INDEX IF NOT EXISTS idx_variants_product_active ON product_variants(product_id, is_active);

-- -- Index cho tìm kiếm theo SKU
-- -- Sử dụng trong: Quản lý kho hàng theo mã SKU
-- CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku);

-- -- Index cho lọc theo default variant
-- -- Sử dụng trong: Hiển thị variant mặc định của sản phẩm
-- CREATE INDEX IF NOT EXISTS idx_variants_product_default ON product_variants(product_id, is_default);

-- -- Index cho tìm kiếm theo giá
-- -- Sử dụng trong: Filter sản phẩm theo khoảng giá
-- CREATE INDEX IF NOT EXISTS idx_variants_price ON product_variants(price);

-- -- Index cho stock quantity
-- -- Sử dụng trong: Kiểm tra tồn kho
-- CREATE INDEX IF NOT EXISTS idx_variants_stock ON product_variants(stock_quantity);

-- -- =====================================
-- -- 7. CATEGORIES TABLE INDEXES
-- -- =====================================

-- -- Index cho tìm kiếm theo slug
-- -- Sử dụng trong: CategoryRepository.findBySlug()
-- CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);

-- -- Index cho filter active categories
-- -- Sử dụng trong: CategoryRepository.findByIsActiveTrue()
-- CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active);

-- -- =====================================
-- -- 8. SUPPLIERS TABLE INDEXES  
-- -- =====================================

-- -- Index cho tìm kiếm supplier theo tên
-- -- Sử dụng trong: SupplierRepository.findByNameContainingIgnoreCase()
-- CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(name);

-- -- Index cho filter featured suppliers
-- -- Sử dụng trong: SupplierRepository.findByIsFeatured()
-- CREATE INDEX IF NOT EXISTS idx_suppliers_featured ON suppliers(is_featured);

-- -- =====================================
-- -- 9. ORDER_DETAILS TABLE INDEXES
-- -- =====================================

-- -- Index cho lấy chi tiết theo order
-- -- Sử dụng trong: Hiển thị chi tiết đơn hàng
-- CREATE INDEX IF NOT EXISTS idx_order_details_order_id ON order_details(order_id);

-- -- Index cho thống kê theo variant
-- -- Sử dụng trong: Báo cáo sản phẩm bán chạy
-- CREATE INDEX IF NOT EXISTS idx_order_details_variant ON order_details(variant_id);

-- -- =====================================
-- -- 10. ADDITIONAL INDEXES CHO CÁC BẢNG KHÁC
-- -- =====================================

-- -- VOUCHERS
-- CREATE INDEX IF NOT EXISTS idx_vouchers_code ON vouchers(code);
-- CREATE INDEX IF NOT EXISTS idx_vouchers_active_dates ON vouchers(is_active, start_date, end_date);

-- -- REFRESH_TOKENS
-- CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
-- CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

-- -- PRODUCT_IMAGES
-- CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);

-- -- BANNERS  
-- CREATE INDEX IF NOT EXISTS idx_banners_active_position ON banners(is_active, position);

-- -- ADDRESSES
-- CREATE INDEX IF NOT EXISTS idx_addresses_user ON addresses(user_id);

-- -- ORDER_STATUS_HISTORY
-- CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history(order_id);
-- CREATE INDEX IF NOT EXISTS idx_order_status_history_time ON order_status_history(changed_at DESC);

-- -- CONVERSATIONS & MESSAGES
-- CREATE INDEX IF NOT EXISTS idx_conversations_participants ON conversations(participant1_id, participant2_id);
-- CREATE INDEX IF NOT EXISTS idx_messages_conversation_time ON messages(conversation_id, sent_at DESC);

-- -- SUPPLIER_MEDIA
-- CREATE INDEX IF NOT EXISTS idx_supplier_media_supplier ON supplier_media(supplier_id);

-- -- REVIEW_MEDIA
-- CREATE INDEX IF NOT EXISTS idx_review_media_review ON review_media(product_review_id);

-- -- PROMOTION_PRODUCTS
-- CREATE INDEX IF NOT EXISTS idx_promotion_products_campaign ON promotion_products(promotion_campaign_id);
-- CREATE INDEX IF NOT EXISTS idx_promotion_products_product ON promotion_products(product_id);

-- -- =====================================
-- -- THÔNG BÁO HOÀN THÀNH
-- -- =====================================
-- -- Tất cả các index đã được tạo thành công!
-- -- Các index này sẽ cải thiện đáng kể hiệu suất của:
-- -- 1. Tìm kiếm và lọc sản phẩm
-- -- 2. Quản lý giỏ hàng  
-- -- 3. Truy vấn đơn hàng
-- -- 4. Hiển thị đánh giá sản phẩm
-- -- 5. Xác thực và quản lý user
-- -- 6. Các truy vấn thống kê và báo cáo
-- -- =====================================
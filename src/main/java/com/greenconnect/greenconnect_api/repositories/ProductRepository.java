package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Product;

/**
 * Repository interface cho thực thể Product.
 * 
 * <p>Quản lý các sản phẩm gốc (không bao gồm các biến thể). Cung cấp các phương thức
 * CRUD cơ bản cho Admin và các chức năng tìm kiếm, lọc mạnh mẽ cho cả người dùng và Admin.
 * Kế thừa {@link JpaSpecificationExecutor} là cốt lõi để xây dựng các bộ lọc động
 * (theo danh mục, khoảng giá, nhà cung cấp, từ khóa...).</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    /**
     * Kiểm tra sự tồn tại của một sản phẩm dựa trên slug.
     */
    boolean existsBySlug(String slug);
    
    /**
     * Kiểm tra tên sản phẩm đã tồn tại chưa (cho validation)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Kiểm tra tên sản phẩm đã tồn tại chưa (exclude product hiện tại - dùng cho update)
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, java.util.UUID id);
    
    /**
     * Kiểm tra slug đã tồn tại chưa (exclude product hiện tại - dùng cho update)
     */
    boolean existsBySlugAndIdNot(String slug, java.util.UUID id);

       org.springframework.data.domain.Page<Product> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm product với eager loading các quan hệ cần thiết
     * Sử dụng JOIN FETCH để tránh N+1 query problem
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.supplier " +
           "WHERE p.id = :productId")
    Optional<Product> findByIdWithDetails(@Param("productId") UUID productId);

    /**
     * Tìm product active với eager loading
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.supplier " +
           "WHERE p.id = :productId AND p.isActive = true")
    Optional<Product> findActiveByIdWithDetails(@Param("productId") UUID productId);
    
    /**
     * Lấy sản phẩm mới nhất (active) - sắp xếp theo createdAt DESC
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findLatestActiveProducts(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm bán chạy (active) - sắp xếp theo sellNumber DESC (số lượng đã bán)
     * ✅ FIX: Dùng sellNumber thay vì reviewCount
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.sellNumber DESC, p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findBestSellingActiveProducts(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm nổi bật (active + featured) - sắp xếp theo createdAt DESC
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findFeaturedActiveProducts(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy top sản phẩm active để làm context cho AI Chatbot
     * Sắp xếp theo số lượt bán giảm dần (sản phẩm bán chạy nhất)
     */
    @Query(value = "SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.sellNumber DESC")
    List<Product> findProductsForAiContext();
    
    /**
     * Lấy sản phẩm đa dạng theo danh mục cho AI Chatbot
     * Dùng window function ROW_NUMBER() để lấy tối đa N sản phẩm mỗi category
     * Ưu tiên sản phẩm bán chạy trong mỗi category
     */
    @Query(value = 
        "SELECT p.* FROM ( " +
        "  SELECT p.*, " +
        "         ROW_NUMBER() OVER (PARTITION BY p.category_id ORDER BY p.sell_number DESC) as rn " +
        "  FROM products p " +
        "  WHERE p.is_active = true " +
        ") p " +
        "WHERE p.rn <= :maxPerCategory " +
        "ORDER BY p.category_id, p.sell_number DESC " +
        "LIMIT :totalLimit",
        nativeQuery = true)
    List<Product> findDiverseProductsForAi(@org.springframework.data.repository.query.Param("maxPerCategory") int maxPerCategory, 
                                           @org.springframework.data.repository.query.Param("totalLimit") int totalLimit);
    
    /**
     * 🔥 Lấy sản phẩm MỚI NHẤT cho AI (sắp xếp theo created_at DESC)
     */
    @Query(value = 
        "SELECT p.* FROM products p " +
        "WHERE p.is_active = true " +
        "ORDER BY p.created_at DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Product> findNewProductsForAi(@org.springframework.data.repository.query.Param("limit") int limit);
    
    /**
     * 🔥 Lấy sản phẩm BÁN CHẠY NHẤT cho AI (sắp xếp theo sell_number DESC)
     */
    @Query(value = 
        "SELECT p.* FROM products p " +
        "WHERE p.is_active = true " +
        "ORDER BY p.sell_number DESC, p.created_at DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Product> findTrendingProductsForAi(@org.springframework.data.repository.query.Param("limit") int limit);
    
    /**
     * 🔥 Lấy sản phẩm ĐÁNH GIÁ CAO NHẤT cho AI (sắp xếp theo average_rating DESC)
     */
    @Query(value = 
        "SELECT p.* FROM products p " +
        "WHERE p.is_active = true AND p.review_count >= 5 " +
        "ORDER BY p.average_rating DESC, p.review_count DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Product> findTopRatedProductsForAi(@org.springframework.data.repository.query.Param("limit") int limit);
    
    /**
     * 🔥 Lấy sản phẩm NỔI BẬT cho AI (is_featured = true)
     */
    @Query(value = 
        "SELECT p.* FROM products p " +
        "WHERE p.is_active = true AND p.is_featured = true " +
        "ORDER BY p.sell_number DESC, p.created_at DESC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Product> findFeaturedProductsForAi(@org.springframework.data.repository.query.Param("limit") int limit);
    
    /**
     * Lấy sản phẩm trong campaign FLASH_SALE active
     * ⚠️ MySQL DISTINCT issue: ORDER BY columns must be in SELECT
     * Solution: Use subquery or remove unnecessary DISTINCT
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.id IN ( " +
           "  SELECT DISTINCT pp.product.id FROM PromotionProduct pp " +
           "  INNER JOIN PromotionCampaign pc ON pp.campaign.id = pc.id " +
           "  WHERE p.isActive = true " +
           "    AND pc.isActive = true " +
           "    AND pc.campaignType = 'FLASH_SALE' " +
           "    AND pc.startDate <= CURRENT_TIMESTAMP " +
           "    AND pc.endDate >= CURRENT_TIMESTAMP " +
           ") " +
           "ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findFlashSaleProducts(org.springframework.data.domain.Pageable pageable);

    /**
    //  * Tìm kiếm một sản phẩm dựa trên slug (chuỗi định danh thân thiện với URL).
    //  * <p>Phương thức này hữu ích cho việc xây dựng các đường dẫn URL đẹp và tốt cho SEO.
    //  * Ví dụ: /products/ca-rot-da-lat thay vì /products/uuid-cua-ca-rot.</p>
    //  *
    //  * @param slug Chuỗi slug duy nhất của sản phẩm.
    //  * @return một đối tượng {@link Optional} chứa {@link Product} nếu tìm thấy.
    //  */
    // Optional<Product> findBySlug(String slug);

    // /**
    //  * Kiểm tra sự tồn tại của một sản phẩm dựa trên slug.
    //  * <p>Rất quan trọng khi Admin tạo hoặc cập nhật sản phẩm để đảm bảo mỗi sản phẩm
    //  * có một đường dẫn URL duy nhất, tránh lỗi 404 hoặc nội dung trùng lặp.</p>
    //  *
    //  * @param slug Chuỗi slug cần kiểm tra.
    //  * @return {@code true} nếu slug đã tồn tại, {@code false} nếu chưa.
    //  */
    // boolean existsBySlug(String slug);

    // /**
    //  * Tìm kiếm các sản phẩm có tên chứa một từ khóa nào đó.
    //  * <p>Đây là một phương thức tìm kiếm cơ bản. Tuy nhiên, chức năng tìm kiếm đầy đủ
    //  * sẽ được xử lý hiệu quả hơn thông qua JpaSpecificationExecutor, cho phép kết hợp
    //  * tìm kiếm theo tên, mô tả, và các bộ lọc khác.</p>
    //  *
    //  * @param name     Từ khóa để tìm kiếm trong tên sản phẩm.
    //  * @param pageable Thông tin phân trang.
    //  * @return một trang (Page) chứa danh sách các sản phẩm phù hợp.
    //  */
    // Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Tìm kiếm các sản phẩm có đánh giá cao nhất kèm đánh giá hàng đầu
     * <p>Phương thức này trả về các sản phẩm được sắp xếp theo:</p>
     * <ol>
     *   <li>Chỉ lấy sản phẩm CÓ đánh giá (có ít nhất 1 review approved)</li>
     *   <li>Ưu tiên sản phẩm có review có media (ảnh/video) lên đầu</li>
     *   <li>Sau đó mới đến đánh giá trung bình (giảm dần)</li>
     * </ol>
     * <p>Mỗi sản phẩm sẽ bao gồm đánh giá cao nhất (highest rated) có chứa hình ảnh và thông tin người dùng.</p>
     *
     * @param pageable Thông tin phân trang.
     * @return một trang (Page) chứa các sản phẩm với đánh giá hàng đầu.
     */
    @Query(value = """
        SELECT DISTINCT p.*, 
               CASE WHEN EXISTS (
                   SELECT 1 FROM product_reviews pr3 
                   JOIN media_reviews mr3 ON mr3.review_id = pr3.id 
                   WHERE pr3.product_id = p.id AND pr3.is_approved = true
               ) THEN 1 ELSE 0 END AS has_review_with_media
        FROM products p
        INNER JOIN product_reviews pr ON pr.product_id = p.id 
          AND pr.is_approved = true
          AND pr.id = (
            SELECT pr2.id FROM product_reviews pr2 
            WHERE pr2.product_id = p.id 
              AND pr2.is_approved = true
            ORDER BY 
              CASE WHEN EXISTS (SELECT 1 FROM media_reviews mr WHERE mr.review_id = pr2.id) THEN 0 ELSE 1 END,
              pr2.rating DESC, 
              pr2.review_time DESC
            LIMIT 1
          )
        WHERE p.is_active = true
        ORDER BY p.average_rating DESC, has_review_with_media DESC, p.review_count DESC, p.created_at DESC
        """, 
        nativeQuery = true,
        countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM products p
            INNER JOIN product_reviews pr ON pr.product_id = p.id AND pr.is_approved = true
            WHERE p.is_active = true
        """)
    org.springframework.data.domain.Page<Product> findTopRatedProductsWithTopReview(org.springframework.data.domain.Pageable pageable);
    
    /*
     * LƯU Ý QUAN TRỌG VỀ TÌM KIẾM VÀ LỌC CHO NGƯỜI DÙNG & ADMIN:
     *
     * Toàn bộ chức năng lọc sản phẩm phức tạp trên trang web (ví dụ: tìm kiếm theo từ khóa,
     * lọc theo danh mục, lọc theo nhà cung cấp, lọc theo khoảng giá, lọc theo chứng nhận...)
     * sẽ được triển khai bằng cách sử dụng JpaSpecificationExecutor.
     *
     * Lớp ProductService sẽ nhận một DTO chứa các tiêu chí lọc từ Controller, sau đó
     * xây dựng một đối tượng Specification linh hoạt để truy vấn. Cách tiếp cận này
     * giữ cho Repository sạch sẽ và cho phép tạo ra các bộ lọc cực kỳ mạnh mẽ.
     *
     * Phương thức được sử dụng sẽ là: findAll(Specification<Product> spec, Pageable pageable)
     */
    
    /**
     * Lấy sản phẩm trong campaign promotion theo slug
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "INNER JOIN PromotionProduct pp ON p.id = pp.product.id " +
           "INNER JOIN PromotionCampaign pc ON pp.campaign.id = pc.id " +
           "WHERE p.isActive = true " +
           "  AND pc.slug = :slug " +
           "  AND pc.isActive = true " +
           "  AND pc.startDate <= CURRENT_TIMESTAMP " +
           "  AND pc.endDate >= CURRENT_TIMESTAMP " +
           "ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findProductsByCampaignSlug(
        @org.springframework.data.repository.query.Param("slug") String slug,
        org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm theo danh mục và nhà cung cấp (cho advanced filtering)
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.isActive = true " +
           "  AND p.category.id IN :categoryIds " +
           "  AND p.supplier.id IN :supplierIds " +
           "ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findByCategoriesAndSuppliers(
        @org.springframework.data.repository.query.Param("categoryIds") List<java.util.UUID> categoryIds,
        @org.springframework.data.repository.query.Param("supplierIds") List<java.util.UUID> supplierIds,
        org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm theo danh mục (cho advanced filtering)
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.isActive = true " +
           "  AND p.category.id IN :categoryIds " +
           "ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findByCategories(
        @org.springframework.data.repository.query.Param("categoryIds") List<java.util.UUID> categoryIds,
        org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm theo nhà cung cấp (cho advanced filtering)
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.isActive = true " +
           "  AND p.supplier.id IN :supplierIds " +
           "ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findBySuppliers(
        @org.springframework.data.repository.query.Param("supplierIds") List<java.util.UUID> supplierIds,
        org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy sản phẩm đánh giá cao (active)
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.averageRating >= 4 ORDER BY p.averageRating DESC")
    org.springframework.data.domain.Page<Product> findHighRatingActiveProducts(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Lấy tất cả sản phẩm active sắp xếp theo createdAt DESC
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    org.springframework.data.domain.Page<Product> findAllByIsActiveTrueOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    // =====================================================
    // 🔍 SEARCH QUERIES (thay thế Elasticsearch bằng LIKE)
    // =====================================================

    /**
     * Tìm kiếm cơ bản theo keyword (LIKE trên name, description, category name, supplier name)
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN p.supplier s " +
           "WHERE p.isActive = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Product> searchByKeyword(
        @Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm kiếm theo keyword + category IDs
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN p.supplier s " +
           "WHERE p.isActive = true " +
           "AND p.category.id IN :categoryIds " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Product> searchByKeywordAndCategories(
        @Param("keyword") String keyword,
        @Param("categoryIds") List<UUID> categoryIds,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm kiếm theo keyword + supplier IDs
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN p.supplier s " +
           "WHERE p.isActive = true " +
           "AND p.supplier.id IN :supplierIds " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Product> searchByKeywordAndSuppliers(
        @Param("keyword") String keyword,
        @Param("supplierIds") List<UUID> supplierIds,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm kiếm theo keyword + category IDs + supplier IDs
     */
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN p.supplier s " +
           "WHERE p.isActive = true " +
           "AND p.category.id IN :categoryIds " +
           "AND p.supplier.id IN :supplierIds " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    org.springframework.data.domain.Page<Product> searchByKeywordAndCategoriesAndSuppliers(
        @Param("keyword") String keyword,
        @Param("categoryIds") List<UUID> categoryIds,
        @Param("supplierIds") List<UUID> supplierIds,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm kiếm sản phẩm active (không keyword)
     */
    org.springframework.data.domain.Page<Product> findByIsActiveTrue(org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm theo category + active
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.category.id = :categoryId")
    org.springframework.data.domain.Page<Product> findByCategoryIdAndIsActiveTrue(
        @Param("categoryId") UUID categoryId, org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm theo supplier + active
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.supplier.id = :supplierId")
    org.springframework.data.domain.Page<Product> findBySupplierIdAndIsActiveTrue(
        @Param("supplierId") UUID supplierId, org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm theo khoảng giá (dựa trên variant price)
     */
    @Query(value = "SELECT DISTINCT p.* FROM products p " +
           "INNER JOIN product_variants pv ON pv.product_id = p.id AND pv.is_active = true " +
           "WHERE p.is_active = true " +
           "AND pv.price BETWEEN :minPrice AND :maxPrice " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT p.id) FROM products p " +
           "INNER JOIN product_variants pv ON pv.product_id = p.id AND pv.is_active = true " +
           "WHERE p.is_active = true AND pv.price BETWEEN :minPrice AND :maxPrice",
           nativeQuery = true)
    org.springframework.data.domain.Page<Product> findByPriceRangeAndIsActiveTrue(
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm sản phẩm featured + active
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.averageRating DESC, p.reviewCount DESC")
    org.springframework.data.domain.Page<Product> findByIsFeaturedTrueAndIsActiveTrue(org.springframework.data.domain.Pageable pageable);

    /**
     * Tìm sản phẩm còn hàng (có variant active với stock > 0)
     */
    @Query(value = "SELECT DISTINCT p.* FROM products p " +
           "INNER JOIN product_variants pv ON pv.product_id = p.id AND pv.is_active = true " +
           "WHERE p.is_active = true AND pv.stock_quantity > 0 " +
           "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(DISTINCT p.id) FROM products p " +
           "INNER JOIN product_variants pv ON pv.product_id = p.id AND pv.is_active = true " +
           "WHERE p.is_active = true AND pv.stock_quantity > 0",
           nativeQuery = true)
    org.springframework.data.domain.Page<Product> findInStockAndIsActiveTrue(org.springframework.data.domain.Pageable pageable);

    /**
     * Đếm tổng sản phẩm active
     */
    long countByIsActiveTrue();

    /**
     * Gợi ý tìm kiếm (tìm tên sản phẩm LIKE keyword)
     */
    @Query("SELECT DISTINCT p.name FROM Product p WHERE p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<String> findProductNameSuggestions(@Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}

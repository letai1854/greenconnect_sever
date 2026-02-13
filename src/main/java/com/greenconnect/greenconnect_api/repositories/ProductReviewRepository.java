package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.ProductReview;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {

    boolean existsByOrderDetailId(UUID orderDetailId);

    // ⭐ Lấy review CỤ THỂ theo orderDetailId (1 OrderDetail = 1 Review)
    @Query("SELECT pr FROM ProductReview pr " +
           "LEFT JOIN FETCH pr.reviewMedia " +
           "LEFT JOIN FETCH pr.user " +
           "LEFT JOIN FETCH pr.shopReplier " +
           "WHERE pr.orderDetail.id = :orderDetailId")
    java.util.Optional<ProductReview> findByOrderDetailIdWithDetails(@Param("orderDetailId") UUID orderDetailId);

    Page<ProductReview> findByProductIdAndIsApprovedTrueOrderByReviewTimeDesc(UUID productId, Pageable pageable);
    
    // ⭐ NEW: Lấy tất cả reviews của product (bao gồm cả chưa duyệt) với media eager load
    @Query("SELECT DISTINCT pr FROM ProductReview pr " +
           "LEFT JOIN FETCH pr.reviewMedia " +
           "LEFT JOIN FETCH pr.user " +
           "LEFT JOIN FETCH pr.shopReplier " +
           "WHERE pr.product.id = :productId " +
           "ORDER BY pr.reviewTime DESC")
    List<ProductReview> findAllByProductIdWithMediaAndUsers(@Param("productId") UUID productId);
    
    // ⭐ NEW: Lấy reviews của product theo userId cụ thể (để lọc reviews của user hiện tại)
    @Query("SELECT DISTINCT pr FROM ProductReview pr " +
           "LEFT JOIN FETCH pr.reviewMedia " +
           "LEFT JOIN FETCH pr.user " +
           "LEFT JOIN FETCH pr.shopReplier " +
           "WHERE pr.product.id = :productId " +
           "AND pr.user.id = :userId " +
           "ORDER BY pr.reviewTime DESC")
    List<ProductReview> findByProductIdAndUserIdWithMediaAndUsers(@Param("productId") UUID productId, @Param("userId") UUID userId);

    long countByProductIdAndIsApprovedTrue(UUID productId);

    // ⭐ MỚI: Dùng JPQL để tính tổng số sao của các review đã được duyệt
    @Query("SELECT SUM(pr.rating) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.isApproved = true")
    Long sumRatingByProductIdAndIsApprovedTrue(@Param("productId") UUID productId);
    
    /**
     * Admin search reviews theo keyword (tên sản phẩm hoặc tên user)
     * Eager load product, user, shopReplier, media
     * 
     * ⚠️ KHÔNG EAGER LOAD productVariants để tránh MultipleBagFetchException
     * Product variants sẽ được lazy load khi cần (trong mapToAdminReviewResponse)
     */
    @Query("SELECT DISTINCT pr FROM ProductReview pr " +
           "LEFT JOIN FETCH pr.product p " +
           "LEFT JOIN FETCH pr.user u " +
           "LEFT JOIN FETCH pr.reviewMedia rm " +
           "LEFT JOIN FETCH pr.shopReplier sr " +
           "WHERE (:keyword IS NULL OR " +
           "      LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "      LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:rating IS NULL OR pr.rating = :rating) " +
           "AND (:hasMedia IS NULL OR " +
           "     (:hasMedia = true AND SIZE(pr.reviewMedia) > 0) OR " +
           "     (:hasMedia = false AND SIZE(pr.reviewMedia) = 0)) " +
           "AND (:replyStatus IS NULL OR " +
           "     (:replyStatus = 'replied' AND pr.shopReply IS NOT NULL) OR " +
           "     (:replyStatus = 'not_replied' AND pr.shopReply IS NULL)) " +
           "ORDER BY pr.reviewTime DESC")
    Page<ProductReview> searchAndFilterReviews(
            @Param("keyword") String keyword,
            @Param("replyStatus") String replyStatus,
            @Param("rating") Integer rating,
            @Param("hasMedia") Boolean hasMedia,
            Pageable pageable);
}

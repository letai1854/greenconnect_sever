package com.greenconnect.greenconnect_api.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.CreateReviewRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateShopReplyRequest;
import com.greenconnect.greenconnect_api.dtos.response.AdminReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.ProductReviewService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ProductReviewController {
    
    private final ProductReviewService reviewService;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;

    /**
     * Tạo đánh giá mới cho sản phẩm
     */
    @PostMapping("/create/{productId}")
    
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @Valid @RequestBody CreateReviewRequest request) {
        
        log.info("User {} đang tạo review cho sản phẩm {}", currentUser.getUserId(), productId);
        
        ProductReviewResponse newReview = reviewService.createReview(productId, currentUser.getUserId(), request);
        
        // 🔄 Track rating in Recombee (quy đổi 1-5 sao sang -1.0 đến 1.0)
        if (recombeeSyncService != null && request.getRating() != null) {
            try {
                recombeeSyncService.trackRating(currentUser.getUserId(), productId, request.getRating().intValue());
                log.info("✅ [RECOMBEE TRACKING] Rating tracked → user={}, product={}, stars={}", 
                    currentUser.getUserId(), productId, request.getRating());
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track rating: {}", e.getMessage());
            }
        } else if (recombeeSyncService == null) {
            log.warn("⚠️ [RECOMBEE] Service not available - Rating tracking skipped");
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(newReview, "Gửi đánh giá thành công."));
    }
    
    /**
     * Shop phản hồi đánh giá
     */
    @PostMapping("/reply/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createShopReply(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @Valid @RequestBody CreateShopReplyRequest request) {
        
        log.info("User {} (roles: {}) đang phản hồi review {}", 
                currentUser.getUserId(), currentUser.getRoles(), reviewId);
        
        ProductReviewResponse updatedReview = reviewService.createShopReply(reviewId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ResponseUtil.success(updatedReview, "Phản hồi thành công."));
    }

    /**
     * Lấy danh sách đánh giá của sản phẩm
     */
    @GetMapping("/getReviews/{productId}")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getReviewsByProduct(
            @PathVariable UUID productId,
            Pageable pageable) {
        
        log.info("Lấy danh sách đánh giá cho sản phẩm {} với pageable: {}", productId, pageable);
        
        Page<ProductReviewResponse> reviewPage = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(ResponseUtil.success(reviewPage, "Lấy danh sách đánh giá thành công."));
    }

    /**
     * Lấy danh sách đánh giá với bộ lọc nâng cao
     * @param productId ID sản phẩm
     * @param rating Lọc theo số sao (1-5), null = tất cả
     * @param hasComment true = CHỈ reviews có comment KHÔNG CÓ ảnh/video, false/null = tất cả
     * @param hasMedia true = CHỈ reviews có ảnh/video (có thể có hoặc không có comment), false/null = tất cả
     * @param sortBy "rating_asc" = sao tăng dần, "rating_desc" = sao giảm dần, null/other = mới nhất
     * @param pageable Phân trang
     * @apiNote hasComment và hasMedia KHÔNG NÊN dùng cùng lúc = true (sẽ trả về empty)
     */
    @GetMapping("/filter/{productId}")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getReviewsWithFilter(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(required = false) String sortBy,
            Pageable pageable) {
        
        log.info("Lọc đánh giá sản phẩm {} - rating: {}, hasComment: {}, hasMedia: {}, sortBy: {}", 
                productId, rating, hasComment, hasMedia, sortBy);
        
        Page<ProductReviewResponse> reviewPage = reviewService.getReviewsWithFilter(
                productId, rating, hasComment, hasMedia, sortBy, pageable);
        return ResponseEntity.ok(ResponseUtil.success(reviewPage, "Lọc đánh giá thành công."));
    }

    /**
     * Customer sửa đánh giá của mình (chỉ trong vòng 3 ngày kể từ khi tạo)
     */
    @PutMapping("/update/{reviewId}")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @Valid @RequestBody CreateReviewRequest request) {
        
        log.info("User {} đang sửa review {}", currentUser.getUserId(), reviewId);
        
        ProductReviewResponse updatedReview = reviewService.updateReview(reviewId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ResponseUtil.success(updatedReview, "Cập nhật đánh giá thành công."));
    }

    /**
     * Sửa phản hồi của shop (chỉ admin hoặc customer support)
     */
    @PutMapping("/reply/update/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateShopReply(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @Valid @RequestBody CreateShopReplyRequest request) {
        
        log.info("User {} (roles: {}) đang sửa phản hồi review {}", 
                currentUser.getUserId(), currentUser.getRoles(), reviewId);
        
        ProductReviewResponse updatedReview = reviewService.updateShopReply(reviewId, currentUser.getUserId(), request);
        return ResponseEntity.ok(ResponseUtil.success(updatedReview, "Cập nhật phản hồi thành công."));
    }

    /**
     * Xóa đánh giá (chỉ admin hoặc customer support)
     */
    @DeleteMapping("/delete/{reviewId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        
        log.info("User {} (roles: {}) đang xóa review {}", 
                currentUser.getUserId(), currentUser.getRoles(), reviewId);
        
        reviewService.deleteReview(reviewId, currentUser.getUserId());
        return ResponseEntity.ok(ResponseUtil.success(null, "Xóa đánh giá thành công."));
    }
    
    /**
     * Admin tìm kiếm và lọc đánh giá
     * 
     * @param keyword Tìm theo tên sản phẩm hoặc tên khách hàng (null hoặc empty = tất cả)
     * @param replyStatus Lọc theo trạng thái phản hồi: "all" (tất cả), "replied" (đã trả lời), "not_replied" (chưa trả lời)
     * @param rating Lọc theo số sao: "all"/"" (tất cả), "1", "2", "3", "4", "5"
     * @param hasMedia Lọc theo có ảnh/video: true (có media), false (không có), null (tất cả)
     * @param pageable Phân trang (page, size, sort)
     * @return Page<AdminReviewResponse> với thông tin đầy đủ sản phẩm, user, review, shop reply
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<AdminReviewResponse>>> adminSearchReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String replyStatus,
            @RequestParam(required = false, defaultValue = "all") String rating,
            @RequestParam(required = false) Boolean hasMedia,
            Pageable pageable) {
        
        log.info("🔍 Admin search reviews - keyword: '{}', replyStatus: '{}', rating: '{}', hasMedia: {}", 
                keyword, replyStatus, rating, hasMedia);
        
        // Parse rating từ String sang Integer
        Integer ratingValue = null;
        if (rating != null && !rating.equalsIgnoreCase("all") && !rating.isEmpty()) {
            try {
                ratingValue = Integer.parseInt(rating);
                if (ratingValue < 1 || ratingValue > 5) {
                    ratingValue = null; // Invalid range, treat as "all"
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid rating value: '{}', treating as 'all'", rating);
                ratingValue = null;
            }
        }
        
        Page<AdminReviewResponse> reviewPage = reviewService.searchAndFilterReviews(
                keyword, replyStatus, ratingValue, hasMedia, pageable);
        
        return ResponseEntity.ok(ResponseUtil.success(reviewPage, "Tìm kiếm đánh giá thành công."));
    }
}

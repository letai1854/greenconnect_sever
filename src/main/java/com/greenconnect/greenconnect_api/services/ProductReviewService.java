package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.CreateReviewRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateShopReplyRequest;
import com.greenconnect.greenconnect_api.dtos.response.AdminReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;

public interface ProductReviewService {
    ProductReviewResponse createReview(UUID productId, UUID userId, CreateReviewRequest request);
    ProductReviewResponse createShopReply(UUID reviewId, UUID replierId, CreateShopReplyRequest request);
    Page<ProductReviewResponse> getReviewsByProduct(UUID productId, Pageable pageable);
    Page<ProductReviewResponse> getReviewsWithFilter(UUID productId, Integer rating, Boolean hasComment, 
                                                      Boolean hasMedia, String sortBy, Pageable pageable);
    ProductReviewResponse updateReview(UUID reviewId, UUID userId, CreateReviewRequest request);
    ProductReviewResponse updateShopReply(UUID reviewId, UUID userId, CreateShopReplyRequest request);
    void deleteReview(UUID reviewId, UUID userId);
    
    /**
     * Admin search và filter reviews
     * @param keyword Tìm theo tên sản phẩm hoặc tên khách hàng (null = tất cả)
     * @param replyStatus "all" | "replied" | "not_replied" (null = tất cả)
     * @param rating 1-5 sao (null = tất cả)
     * @param hasMedia true = có ảnh/video, false/null = tất cả
     * @param pageable Phân trang
     * @return Page of AdminReviewResponse
     */
    Page<AdminReviewResponse> searchAndFilterReviews(String keyword, String replyStatus, 
                                                      Integer rating, Boolean hasMedia, Pageable pageable);
}
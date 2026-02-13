package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.CreateReviewRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateShopReplyRequest;
import com.greenconnect.greenconnect_api.dtos.response.AdminReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.entities.OrderDetail;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductReview;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.ReviewMedia;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.MediaType;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.mappers.ProductReviewMapper;
import com.greenconnect.greenconnect_api.repositories.OrderDetailRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.ProductReviewRepository;
import com.greenconnect.greenconnect_api.repositories.ReviewMediaRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.ProductReviewService;
import com.greenconnect.greenconnect_api.websocket.NewReviewNotification;
import com.greenconnect.greenconnect_api.websocket.NotificationService;
import com.greenconnect.greenconnect_api.websocket.ShopReplyNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReviewServiceImpl implements ProductReviewService {
    
    private final ProductReviewRepository reviewRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final NotificationService notificationService;
    private final ProductReviewMapper reviewMapper;
    private final ProductRepository productRepository;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY
    private ElasticsearchSyncService elasticsearchSyncService;

    @Override
    @Transactional
    public ProductReviewResponse createReview(UUID productId, UUID userId, CreateReviewRequest request) {
        log.info("Tạo đánh giá mới cho sản phẩm {} bởi user {}", productId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        OrderDetail orderDetail = verifyReviewEligibility(productId, userId, request.getOrderDetailId());
        
        // Kiểm tra xem orderDetail đã được review chưa
        if (reviewRepository.existsByOrderDetailId(request.getOrderDetailId())) {
            log.warn("OrderDetail {} đã được review trước đó", request.getOrderDetailId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        ProductReview review = ProductReview.builder()
                .product(orderDetail.getVariant().getProduct())
                .user(user)
                .orderDetail(orderDetail)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        
        ProductReview savedReview = reviewRepository.save(review);
        log.info("Đã lưu review với ID: {}", savedReview.getId());

        // Lưu media nếu có
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            saveReviewMedia(savedReview, request.getMediaList());
        }

        // ⭐ CẬP NHẬT RATING CHO PRODUCT
        updateProductAggregatedRating(orderDetail.getVariant().getProduct());
        
        // 🔄 SYNC VÀO ELASTICSEARCH
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(orderDetail.getVariant().getProduct().getId());
                log.info("✅ Đã sync product {} vào Elasticsearch sau khi thêm review", 
                        orderDetail.getVariant().getProduct().getId());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }

        // Gửi thông báo real-time
        broadcastNewReview(savedReview);

        return reviewMapper.toResponse(savedReview);
    }
    
    @Override
    @Transactional
    public ProductReviewResponse createShopReply(UUID reviewId, UUID replierId, CreateShopReplyRequest request) {
        log.info("Tạo phản hồi cho review {} bởi user {}", reviewId, replierId);
        
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        
        User replier = userRepository.findById(replierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Kiểm tra xem đã có shop reply chưa
        if (review.getShopReply() != null) {
            log.warn("Review {} đã có phản hồi trước đó", reviewId);
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_REPLIED);
        }
        
        review.setShopReply(request.getContent());
        review.setShopReplyAt(LocalDateTime.now());
        review.setShopReplier(replier);
        
        ProductReview updatedReview = reviewRepository.save(review);
        log.info("Đã cập nhật shop reply cho review {}", reviewId);
        
        // Gửi thông báo real-time
        broadcastShopReply(updatedReview);
        
        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getReviewsByProduct(UUID productId, Pageable pageable) {
        log.info("Lấy danh sách review cho sản phẩm {} với pageable: {}", productId, pageable);
        
        Page<ProductReview> reviewPage = reviewRepository
                .findByProductIdAndIsApprovedTrueOrderByReviewTimeDesc(productId, pageable);
        
        return reviewPage.map(reviewMapper::toResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getReviewsWithFilter(UUID productId, Integer rating, 
                                                             Boolean hasComment, Boolean hasMedia, 
                                                             String sortBy, Pageable pageable) {
        log.info("Lọc reviews - productId: {}, rating: {}, hasComment: {}, hasMedia: {}, sortBy: {}", 
                productId, rating, hasComment, hasMedia, sortBy);
        
        // Tạo Specification để lọc
        Specification<ProductReview> spec = Specification.where(null);
        
        // Lọc theo productId và isApproved
        spec = spec.and((root, query, cb) -> 
            cb.and(
                cb.equal(root.get("product").get("id"), productId),
                cb.isTrue(root.get("isApproved"))
            )
        );
        
        // Lọc theo rating (số sao)
        if (rating != null && rating >= 1 && rating <= 5) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rating"), rating));
        }
        
        // Lọc theo hasComment (CHỈ có comment, KHÔNG CÓ media)
        if (hasComment != null && hasComment) {
            spec = spec.and((root, query, cb) -> 
                cb.and(
                    cb.isNotNull(root.get("comment")),
                    cb.notEqual(cb.trim(root.get("comment")), ""),
                    cb.isEmpty(root.get("reviewMedia")) // ⭐ CHỈ LẤY REVIEWS KHÔNG CÓ MEDIA
                )
            );
        }
        
        // Lọc theo hasMedia (CHỈ có ảnh/video, có thể có hoặc không comment)
        if (hasMedia != null && hasMedia) {
            spec = spec.and((root, query, cb) -> 
                cb.isNotEmpty(root.get("reviewMedia"))
            );
        }
        
        // Xử lý sắp xếp
        Sort sort;
        if ("rating_asc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "reviewTime"));
        } else if ("rating_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "reviewTime"));
        } else {
            // Mặc định: mới nhất
            sort = Sort.by(Sort.Direction.DESC, "reviewTime");
        }
        
        // Tạo Pageable mới với sort
        Pageable pageableWithSort = PageRequest.of(
            pageable.getPageNumber(), 
            pageable.getPageSize(), 
            sort
        );
        
        // Thực hiện query
        Page<ProductReview> reviewPage = reviewRepository.findAll(spec, pageableWithSort);
        
        log.info("Tìm thấy {} reviews sau khi lọc", reviewPage.getTotalElements());
        
        return reviewPage.map(reviewMapper::toResponse);
    }
    
    @Override
    @Transactional
    public ProductReviewResponse updateReview(UUID reviewId, UUID userId, CreateReviewRequest request) {
        log.info("Cập nhật review {} bởi user {}", reviewId, userId);
        
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        
        // Kiểm tra quyền: chỉ người tạo review mới được sửa
        if (!review.getUser().getId().equals(userId)) {
            log.warn("User {} không có quyền sửa review {} của user {}", 
                    userId, reviewId, review.getUser().getId());
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // Kiểm tra thời gian: chỉ cho phép sửa trong vòng 3 ngày
        LocalDateTime createdTime = review.getReviewTime();
        LocalDateTime now = LocalDateTime.now();
        long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(createdTime, now);
        
        if (daysDifference > 3) {
            log.warn("Review {} đã quá 3 ngày ({} ngày), không thể sửa", reviewId, daysDifference);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // Cập nhật thông tin review
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        
        // 🔥 XÓA TẤT CẢ MEDIA CŨ qua collection (orphanRemoval sẽ xóa DB tự động)
        review.getReviewMedia().clear();
        
        // 🔥 THÊM MEDIA MỚI qua collection (cascade sẽ save tự động)
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            List<ReviewMedia> newMediaList = new ArrayList<>();
            for (int i = 0; i < request.getMediaList().size(); i++) {
                CreateReviewRequest.MediaRequest mediaRequest = request.getMediaList().get(i);
                
                ReviewMedia media = ReviewMedia.builder()
                        .productReview(review)
                        .mediaType(MediaType.valueOf(mediaRequest.getMediaType().toUpperCase()))
                        .mediaUrl(mediaRequest.getMediaUrl())
                        .displayOrder(i)
                        .build();
                
                newMediaList.add(media);
            }
            
            // Thêm vào collection của review (cascade sẽ persist)
            review.getReviewMedia().addAll(newMediaList);
        }
        
        // 🔥 Chỉ save entity cha - cascade sẽ xử lý entity con
        ProductReview updatedReview = reviewRepository.save(review);
        log.info("Đã cập nhật review {} thành công - Media count: {}", 
                reviewId, updatedReview.getReviewMedia().size());
        
        // Cập nhật lại rating trung bình của sản phẩm
        updateProductAggregatedRating(review.getProduct());
        
        // Sync vào Elasticsearch
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(review.getProduct().getId());
                log.info("✅ Đã sync product {} vào Elasticsearch sau khi cập nhật review", 
                        review.getProduct().getId());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        }
        
        return reviewMapper.toResponse(updatedReview);
    }
    
    @Override
    @Transactional
    public ProductReviewResponse updateShopReply(UUID reviewId, UUID userId, CreateShopReplyRequest request) {
        log.info("Cập nhật shop reply cho review {} bởi user {}", reviewId, userId);
        
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        
        // Kiểm tra xem đã có shop reply chưa
        if (review.getShopReply() == null) {
            log.warn("Review {} chưa có shop reply để cập nhật", reviewId);
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        User replier = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Cập nhật shop reply
        review.setShopReply(request.getContent());
        review.setShopReplyAt(LocalDateTime.now());
        review.setShopReplier(replier);
        
        ProductReview updatedReview = reviewRepository.save(review);
        log.info("Đã cập nhật shop reply cho review {} thành công", reviewId);
        
        return reviewMapper.toResponse(updatedReview);
    }
    
    @Override
    @Transactional
    public void deleteReview(UUID reviewId, UUID userId) {
        log.info("Xóa review {} bởi user {} (Admin/Support)", reviewId, userId);
        
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        
        Product product = review.getProduct();
        
        // Xóa media trước
        reviewMediaRepository.deleteByProductReviewId(reviewId);
        
        // Xóa review
        reviewRepository.delete(review);
        log.info("Đã xóa review {} thành công", reviewId);
        
        // Cập nhật lại rating trung bình của sản phẩm
        updateProductAggregatedRating(product);
        
        // Sync vào Elasticsearch
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(product.getId());
                log.info("✅ Đã sync product {} vào Elasticsearch sau khi xóa review", product.getId());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Xác minh quyền tạo review: User phải đã mua sản phẩm này và đơn hàng đã hoàn thành
     */
    private OrderDetail verifyReviewEligibility(UUID productId, UUID userId, UUID orderDetailId) {
        // 🎯 CÁCH 1: Sử dụng method tối ưu mới (giảm số lần truy vấn DB)
        // ✅ DA_GIAO = Đơn hàng đã giao (hoàn thành) - user có thể review
        Optional<OrderDetail> optionalOrderDetail = orderDetailRepository
                .findByIdAndUserIdAndOrderCompleted(orderDetailId, userId, OrderStatus.DA_GIAO);
        
        if (optionalOrderDetail.isEmpty()) {
            log.warn("Không tìm thấy OrderDetail {} thuộc user {} hoặc đơn hàng chưa hoàn thành", 
                    orderDetailId, userId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        OrderDetail orderDetail = optionalOrderDetail.get();
        
        // Kiểm tra sản phẩm trong orderDetail có khớp với productId không
        if (!orderDetail.getVariant().getProduct().getId().equals(productId)) {
            log.warn("OrderDetail {} không chứa sản phẩm {}, thực tế chứa sản phẩm {}", 
                    orderDetailId, productId, orderDetail.getVariant().getProduct().getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        log.info("✅ Xác thực thành công: User {} có quyền review sản phẩm {} từ OrderDetail {}", 
                userId, productId, orderDetailId);
        
        return orderDetail;
    }
    
    /**
     * Lưu media files cho review
     */
    private void saveReviewMedia(ProductReview review, List<CreateReviewRequest.MediaRequest> mediaRequests) {
        List<ReviewMedia> mediaList = new ArrayList<>();
        
        for (int i = 0; i < mediaRequests.size(); i++) {
            CreateReviewRequest.MediaRequest mediaRequest = mediaRequests.get(i);
            
            ReviewMedia media = ReviewMedia.builder()
                    .productReview(review)
                    .mediaType(MediaType.valueOf(mediaRequest.getMediaType().toUpperCase()))
                    .mediaUrl(mediaRequest.getMediaUrl())
                    .displayOrder(i)
                    .build();
            
            mediaList.add(media);
        }
        
        reviewMediaRepository.saveAll(mediaList);
        log.info("Đã lưu {} media files cho review {}", mediaList.size(), review.getId());
    }
    
    /**
     * 📊 CẬP NHẬT RATING TRUNG BÌNH VÀ SỐ LƯỢNG REVIEW CHO PRODUCT
     */
    private void updateProductAggregatedRating(Product product) {
        try {
            // 1️⃣ Đếm tổng số reviews đã được duyệt
            long reviewCount = reviewRepository.countByProductIdAndIsApprovedTrue(product.getId());
            
            // 2️⃣ Tính toán rating trung bình từ tất cả reviews đã được duyệt
            BigDecimal averageRating = BigDecimal.ZERO;
            if (reviewCount > 0) {
                Long totalRating = reviewRepository.sumRatingByProductIdAndIsApprovedTrue(product.getId());
                if (totalRating != null && totalRating > 0) {
                    averageRating = new BigDecimal(totalRating).divide(new BigDecimal(reviewCount), 2, RoundingMode.HALF_UP);
                }
            }
            
            // 3️⃣ Cập nhật vào database
            product.setAverageRating(averageRating);
            product.setReviewCount((int) reviewCount);
            
            productRepository.save(product);
            
            log.info("✅ Đã cập nhật rating cho product {}: avg={}, count={}", 
                    product.getId(), averageRating, reviewCount);
                    
        } catch (Exception e) {
            log.error("❌ Lỗi cập nhật rating cho product {}: {}", 
                    product.getId(), e.getMessage());
        }
    }
    
    /**
     * Gửi thông báo real-time khi có review mới
     */
    private void broadcastNewReview(ProductReview review) {
        try {
            NewReviewNotification notification = NewReviewNotification.builder()
                    .reviewId(review.getId())
                    .productId(review.getProduct().getId())
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .userName(review.getUser().getFullName())
                    .reviewTime(review.getReviewTime())
                    .build();
            
            notificationService.sendCustomNotification("/topic/reviews/new", notification);
            log.info("Đã gửi thông báo review mới cho sản phẩm {}", review.getProduct().getId());
        } catch (Exception e) {
            log.warn("Lỗi khi gửi thông báo review mới: {}", e.getMessage());
        }
    }
    
    /**
     * Gửi thông báo real-time khi có shop reply
     */
    private void broadcastShopReply(ProductReview review) {
        try {
            ShopReplyNotification notification = ShopReplyNotification.builder()
                    .reviewId(review.getId())
                    .productId(review.getProduct().getId())
                    .replyContent(review.getShopReply())
                    .replierName(review.getShopReplier().getFullName())
                    .repliedAt(review.getShopReplyAt())
                    .build();
            
            notificationService.sendCustomNotification("/topic/reviews/reply", notification);
            log.info("Đã gửi thông báo shop reply cho review {}", review.getId());
        } catch (Exception e) {
            log.warn("Lỗi khi gửi thông báo shop reply: {}", e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> searchAndFilterReviews(String keyword, String replyStatus, 
                                                             Integer rating, Boolean hasMedia, Pageable pageable) {
        log.info("🔍 Admin search reviews - keyword: {}, replyStatus: {}, rating: {}, hasMedia: {}", 
                keyword, replyStatus, rating, hasMedia);
        
        // Chuẩn hóa replyStatus
        String normalizedReplyStatus = null;
        if (replyStatus != null && !replyStatus.equalsIgnoreCase("all")) {
            normalizedReplyStatus = replyStatus.toLowerCase();
        }
        
        // Gọi repository với query tùy chỉnh
        Page<ProductReview> reviewPage = reviewRepository.searchAndFilterReviews(
                keyword, normalizedReplyStatus, rating, hasMedia, pageable);
        
        // Map sang AdminReviewResponse
        return reviewPage.map(this::mapToAdminReviewResponse);
    }
    
    /**
     * Map ProductReview entity sang AdminReviewResponse DTO
     */
    private AdminReviewResponse mapToAdminReviewResponse(ProductReview review) {
        Product product = review.getProduct();
        
        // Lấy variant đầu tiên để lấy giá (hoặc variant default)
        ProductVariant defaultVariant = product.getProductVariants().stream()
                .filter(v -> v.getIsDefault() != null && v.getIsDefault())
                .findFirst()
                .orElse(product.getProductVariants().isEmpty() ? null : product.getProductVariants().get(0));
        
        BigDecimal price = defaultVariant != null ? defaultVariant.getPrice() : BigDecimal.ZERO;
        String unit = defaultVariant != null ? defaultVariant.getUnit() : "N/A";
        
        // Product info
        AdminReviewResponse.ProductInfo productInfo = AdminReviewResponse.ProductInfo.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getMainImageUrl())
                .price(price)
                .unit(unit)
                .build();
        
        // Review info với media
        List<AdminReviewResponse.MediaInfo> mediaList = review.getReviewMedia().stream()
                .map(media -> AdminReviewResponse.MediaInfo.builder()
                        .mediaType(media.getMediaType().name())
                        .mediaUrl(media.getMediaUrl())
                        .build())
                .collect(Collectors.toList());
        
        AdminReviewResponse.ReviewInfo reviewInfo = AdminReviewResponse.ReviewInfo.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewTime(review.getReviewTime())
                .mediaList(mediaList)
                .build();
        
        // User info
        AdminReviewResponse.UserInfo userInfo = AdminReviewResponse.UserInfo.builder()
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .avatarUrl(review.getUser().getAvatarUrl())
                .build();
        
        // Shop reply info (nếu có)
        AdminReviewResponse.ShopReplyInfo shopReplyInfo = null;
        if (review.getShopReply() != null) {
            shopReplyInfo = AdminReviewResponse.ShopReplyInfo.builder()
                    .content(review.getShopReply())
                    .repliedAt(review.getShopReplyAt())
                    .replierName(review.getShopReplier() != null ? review.getShopReplier().getFullName() : "N/A")
                    .build();
        }
        
        return AdminReviewResponse.builder()
                .product(productInfo)
                .review(reviewInfo)
                .user(userInfo)
                .shopReply(shopReplyInfo)
                .build();
    }
}

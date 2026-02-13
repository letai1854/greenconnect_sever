package com.greenconnect.greenconnect_api.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;
import com.greenconnect.greenconnect_api.entities.ProductReview;
import com.greenconnect.greenconnect_api.entities.ReviewMedia;
import com.greenconnect.greenconnect_api.entities.User;

@Component
public class ProductReviewMapper {

    public ProductReviewResponse toResponse(ProductReview review) {
        if (review == null) {
            return null;
        }

        return ProductReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewTime(review.getReviewTime())
                .user(buildUserInfo(review.getUser()))
                .mediaList(buildMediaInfo(review.getReviewMedia()))
                .shopReply(buildShopReplyInfo(review))
                .build();
    }

    private ProductReviewResponse.UserInfo buildUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return ProductReviewResponse.UserInfo.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl()) // Giả sử User entity có trường này
                .build();
    }

    private List<ProductReviewResponse.MediaInfo> buildMediaInfo(List<ReviewMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaList.stream()
                .map(media -> ProductReviewResponse.MediaInfo.builder()
                        .mediaType(media.getMediaType().name())
                        .mediaUrl(media.getMediaUrl())
                        .build())
                .collect(Collectors.toList());
    }

    private ProductReviewResponse.ShopReplyInfo buildShopReplyInfo(ProductReview review) {
        if (review.getShopReply() == null || review.getShopReplier() == null) {
            return null;
        }
        return ProductReviewResponse.ShopReplyInfo.builder()
                .content(review.getShopReply())
                .repliedAt(review.getShopReplyAt())
                .replierName(review.getShopReplier().getFullName())
                .build();
    }
}
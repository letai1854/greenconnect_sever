package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewReviewNotification {
    private UUID reviewId;
    private UUID productId;
    private short rating;
    private String comment;
    private String userName;
    private LocalDateTime reviewTime;
    private List<ProductReviewResponse.MediaInfo> mediaList;
}
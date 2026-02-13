package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductReviewResponse {
    private UUID id;
    private short rating;
    private String comment;
    private LocalDateTime reviewTime;
    private UserInfo user;
    private List<MediaInfo> mediaList;
    private ShopReplyInfo shopReply;

    @Data
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String fullName;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class MediaInfo {
        private String mediaType;
        private String mediaUrl;
    }
    
    @Data
    @Builder
    public static class ShopReplyInfo {
        private String content;
        private LocalDateTime repliedAt;
        private String replierName;
    }
}
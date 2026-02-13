package com.greenconnect.greenconnect_api.websocket;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShopReplyNotification {
    private UUID reviewId;
    private UUID productId;
    private String replyContent;
    private String replierName;
    private LocalDateTime repliedAt;
}
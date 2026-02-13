package com.greenconnect.greenconnect_api.dtos.response;

import com.greenconnect.greenconnect_api.enums.BannerGroupLayoutType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BannerGroupResponse {
    private UUID groupId;
    private String groupName;
    private String groupKey;
    private BannerGroupLayoutType displayLayout;
    private List<BannerDetail> banners;

    @Data
    @Builder
    public static class BannerDetail {
        private UUID bannerId;
        private String bannerName;
        private String imageUrl;
        private String targetUrl;
        private Integer displayOrder;
        private boolean isActive;
        private LocalDateTime createdAt;
    }
}
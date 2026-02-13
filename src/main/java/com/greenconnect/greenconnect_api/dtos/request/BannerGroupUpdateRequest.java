package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.BannerGroupLayoutType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;
import java.util.UUID;

@Data
public class BannerGroupUpdateRequest {

    @NotNull(message = "groupId is required")
    private UUID groupId;

    @NotBlank(message = "groupName is required")
    private String groupName;

    @NotBlank(message = "groupKey is required")
    private String groupKey;

    @NotNull(message = "displayLayout is required")
    private BannerGroupLayoutType displayLayout;

    @Valid
    @NotNull(message = "banners cannot be null")
    @Size(min = 0, message = "banners list may be empty")
    private List<BannerItem> banners;

    @Data
    public static class BannerItem {
        private UUID bannerId; // nullable -> indicates new banner

        @NotBlank(message = "bannerName is required")
        private String bannerName;

        @NotBlank(message = "imageUrl is required")
        @URL(message = "imageUrl invalid")
        private String imageUrl;

        @URL(message = "targetUrl invalid")
        private String targetUrl;

        @NotNull(message = "displayOrder is required")
        @Min(value = 0)
        private Integer displayOrder;

        private boolean active = true;
    }
}

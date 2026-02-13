// BannerDetailResponse.java
package com.greenconnect.greenconnect_api.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class BannerDetailResponse {
    private UUID id;
    private String imageUrl;
    private String targetUrl;
}

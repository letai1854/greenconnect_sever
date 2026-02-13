// ProductSummaryResponse.java
package com.greenconnect.greenconnect_api.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class ProductSummaryResponse {
    private UUID id;
    private String name;
    private String thumbnailUrl;
    private String originalPrice;
    private String finalPrice;
    private String discountLabel;
}
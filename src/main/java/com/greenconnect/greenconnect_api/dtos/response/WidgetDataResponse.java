// WidgetDataResponse.java
package com.greenconnect.greenconnect_api.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WidgetDataResponse {
    private List<BannerDetailResponse> banners;
    // Now contain full product response objects for product lists
    private List<ProductResponse> products;
}

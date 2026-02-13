package com.greenconnect.greenconnect_api.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class HomepageResponse {
    private List<WidgetResponse> widgets;
}
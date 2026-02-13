package com.greenconnect.greenconnect_api.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null khi serialize
public class WidgetResponse {
    private String widgetId;
    private String widgetType;
    private String displayLayout;
    private String title;
    private String viewAllUrl;
    private WidgetDataResponse data;
    private PaginationResponse pagination;
}
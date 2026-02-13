package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.DisplayLayoutType;
import com.greenconnect.greenconnect_api.enums.WidgetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class HomepageLayoutRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    // @NotNull(message = "Loại widget không được để trống")
    // private WidgetType widgetType;

    // For widget types that use a data source (e.g., banner group key or campaign list)
    private String dataSourceKey;

    private Boolean isActive = true;

    // Optional admin-supplied IDs to wire up existing resources
    private UUID bannerGroupId;
    private List<UUID> orderedCampaignIds;
}
// package com.greenconnect.greenconnect_api.dtos.request;

// import com.greenconnect.greenconnect_api.enums.DisplayLayoutType;
// import com.greenconnect.greenconnect_api.enums.WidgetType;
// import jakarta.validation.constraints.Min;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.NotNull;
// import lombok.Data;

// @Data
// public class HomepageLayoutRequest {

//     @NotBlank(message = "Tiêu đề không được để trống")
//     private String title;

//     @NotNull(message = "Loại widget không được để trống")
//     private WidgetType widgetType;

//     // dataSourceKey có thể là null, ví dụ cho các widget cố định nếu bạn muốn quản lý chúng trong DB
//     private String dataSourceKey;

//     @NotNull(message = "Layout hiển thị không được để trống")
//     private DisplayLayoutType displayLayout;

//     @NotNull(message = "Thứ tự hiển thị không được để trống")
//     @Min(value = 0, message = "Thứ tự hiển thị phải là số không âm")
//     private Integer displayOrder;

//     private Boolean isActive = true;
// }
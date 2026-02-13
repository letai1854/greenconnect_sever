package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.HomepageLayoutRequest;
import com.greenconnect.greenconnect_api.dtos.response.HomepageLayoutRespone;
import com.greenconnect.greenconnect_api.dtos.response.HomepageResponse;

public interface HomepageLayoutService {
    HomepageLayoutRespone createLayout(HomepageLayoutRequest request);

    /**
     * Build the full homepage content composed from the active homepage layout(s).
     * For each promotion campaign included in the layout, only the first page (up to 8 products)
     * will be returned to keep the response small and fast.
     */
    HomepageResponse getHomepageContent();

    /**
     * Build the homepage content for a specific layout id. Returns the banner group (if any)
     * and a widget per campaign where each campaign widget includes up to 8 products and
     * pagination link info so clients can request further pages.
     */
    com.greenconnect.greenconnect_api.dtos.response.HomepageResponse getLayoutContent(java.util.UUID layoutId);

    /**
     * Return a paginated set of products for a specific promotion campaign.
     * Page is zero-based and pageSize defaults to 8 in the implementation.
     */
    com.greenconnect.greenconnect_api.dtos.response.WidgetResponse getCampaignProducts(java.util.UUID campaignId, int page, int pageSize);

    // Admin operations to manage associations between a homepage layout and banners/campaigns
    void unlinkBannerFromLayout(java.util.UUID layoutId, java.util.UUID bannerId);

    void linkBannerToLayout(java.util.UUID layoutId, java.util.UUID bannerId);

    void unlinkCampaignFromLayout(java.util.UUID layoutId, java.util.UUID campaignId);

    void linkCampaignsToLayout(java.util.UUID layoutId, java.util.List<java.util.UUID> campaignIds);
}
// package com.greenconnect.greenconnect_api.services;

// import com.greenconnect.greenconnect_api.dtos.response.HomepageResponse;

// public interface HomepageLayoutService {
//     /**
//      * Lấy và xây dựng toàn bộ nội dung cho trang chủ, bao gồm các widget
//      * động từ CSDL và các widget cố định.
//      *
//      * @return một đối tượng HomepageResponse chứa danh sách các widget.
//      */
//     HomepageResponse getHomepageContent();

//     // Admin operations for managing homepage layouts
//     com.greenconnect.greenconnect_api.dtos.response.HomepageLayoutRespone createLayout(com.greenconnect.greenconnect_api.dtos.request.HomepageLayoutRequest request);

//     com.greenconnect.greenconnect_api.dtos.response.HomepageLayoutRespone updateLayout(java.util.UUID layoutId, com.greenconnect.greenconnect_api.dtos.request.HomepageLayoutRequest request);

//     void deleteLayout(java.util.UUID layoutId);
// }
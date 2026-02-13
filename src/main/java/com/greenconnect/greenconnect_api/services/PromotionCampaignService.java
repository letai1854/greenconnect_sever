package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignCreateRequest;
import com.greenconnect.greenconnect_api.dtos.response.PromotionCampaignResponse;

public interface PromotionCampaignService {
    PromotionCampaignResponse createCampaign(PromotionCampaignCreateRequest request);
    
    /**
     * Lấy chi tiết một promotion campaign theo id
     */
    PromotionCampaignResponse getCampaignById(java.util.UUID campaignId);
    
    /**
     * Lấy campaign với product count theo id
     */
    com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse getCampaignWithCountById(java.util.UUID campaignId);

    /**
     * Partial update: update non-null fields and append new promotion products
     */
    PromotionCampaignResponse updateCampaignPartial(java.util.UUID campaignId, com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest request);

    /**
     * Add promotion products (append) to campaign
     */
    PromotionCampaignResponse addProductsToCampaign(java.util.UUID campaignId, java.util.List<com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem> products);

    /**
     * Add products by List of UUIDs to campaign
     */
    PromotionCampaignResponse addProductsByIds(java.util.UUID campaignId, java.util.List<java.util.UUID> productIds);

    /**
     * Remove products by List of UUIDs from campaign
     */
    void removeProductsByIds(java.util.UUID campaignId, java.util.List<java.util.UUID> productIds);

    /**
     * Remove all promotion products from a campaign (unlink/delete)
     */
    void removeAllProductsFromCampaign(java.util.UUID campaignId);

    /**
     * Remove a single promotion product link from a campaign
     */
    void removeProductFromCampaign(java.util.UUID campaignId, java.util.UUID productId);
    
    /**
     * Apply discount percentage to all variants of products in a campaign
     */
    void applyDiscountToProductVariants(java.util.UUID campaignId);
    
    /**
     * Remove discount from all variants of products in a campaign
     */
    void removeDiscountFromProductVariants(java.util.UUID campaignId);
    
    /**
     * Lấy danh sách campaigns active với phân trang
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getActiveCampaignsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy danh sách campaigns inactive với phân trang
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getInactiveCampaignsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy tất cả campaigns với phân trang
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getAllCampaignsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy campaigns còn hiệu lực (valid) với phân trang
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getValidCampaignsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy campaigns đã hết hạn (expired) với phân trang
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getExpiredCampaignsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Tìm kiếm campaigns với phân trang theo tab (active/inactive/all/valid/expired)
     */
    org.springframework.data.domain.Page<PromotionCampaignResponse> searchCampaigns(String keyword, String tab, int page, int size);
    
    /**
     * Lấy campaign mới nhất theo type BULK_PURCHASE còn hiệu lực
     * @return Campaign mới nhất loại BULK_PURCHASE đang còn hiệu lực
     */
    com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse getLatestValidBulkPurchaseCampaign();
    
    /**
     * Lấy danh sách products với phân trang, đánh dấu products đã có trong campaign
     * @param campaignId UUID của campaign
     * @param categoryId UUID của category (null = all categories)
     * @param page Số trang
     * @param size Kích thước trang
     * @return Page<ProductInCampaignResponse> với flag isInCampaign
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.ProductInCampaignResponse> getProductsWithCampaignStatus(
        java.util.UUID campaignId, 
        java.util.UUID categoryId, 
        int page, 
        int size
    );
    
    /**
     * Lấy danh sách TẤT CẢ products với phân trang theo category
     * @param categoryId UUID của category (null = all categories)
     * @param page Số trang
     * @param size Kích thước trang
     * @return Page<ProductResponse> - TẤT CẢ products theo category/all
     */
    org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> getAllProductsByCategory(
        java.util.UUID categoryId, 
        int page, 
        int size
    );
}
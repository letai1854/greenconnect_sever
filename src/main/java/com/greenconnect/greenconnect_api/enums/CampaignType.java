package com.greenconnect.greenconnect_api.enums;

/**
 * Loại chiến dịch khuyến mãi
 * 
 * Campaign Types:
 * - SEASONAL: Chiến dịch theo mùa (Tết, Hè, Thu, Đông)
 * - FLASH_SALE: Bán giảm giá nhanh (thời gian giới hạn)
 * - CLEARANCE: Xóa hàng tồn kho
 * - BRAND_COLLABORATION: Hợp tác với thương hiệu
 * - LOYALTY_PROGRAM: Chương trình khách hàng thân thiết
 * - SEASONAL_HOLIDAY: Ngày lễ đặc biệt (8/3, Giáng sinh, v.v.)
 * - FIRST_TIME_BUYER: Ưu đãi cho khách mua lần đầu
 * - BULK_PURCHASE: Giảm giá mua nhiều
 */
public enum CampaignType {
    SEASONAL("Chiến dịch theo mùa"),
    FLASH_SALE("Bán giảm giá nhanh"),
    CLEARANCE("Xóa hàng tồn kho"),
    BRAND_COLLABORATION("Hợp tác thương hiệu"),
    LOYALTY_PROGRAM("Chương trình thân thiết"),
    SEASONAL_HOLIDAY("Ngày lễ đặc biệt"),
    FIRST_TIME_BUYER("Ưu đãi khách mới"),
    BULK_PURCHASE("Ưu đãi mua nhiều");

    private final String description;

    CampaignType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

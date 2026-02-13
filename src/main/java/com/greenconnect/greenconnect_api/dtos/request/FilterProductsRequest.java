package com.greenconnect.greenconnect_api.dtos.request;

import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.PriceRange;
import com.greenconnect.greenconnect_api.enums.ProductSortType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để lọc sản phẩm theo nhiều tiêu chí
 * ✅ ĐỒNG BỘ VỚI ProductFilterRequest - Dùng sortBy enum thay vì Pageable.sort
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterProductsRequest {
    
    private List<UUID> categoryIds; // Lọc theo danh mục
    
    private List<UUID> supplierIds; // Lọc theo nhà cung cấp
    
    private PriceRange priceRange; // Lọc theo khoảng giá
    
    private Boolean isNew; // Sản phẩm mới nhất
    
    private Boolean isBestSeller; // Sản phẩm bán chạy nhất
    
    private Boolean isOnSale; // Sản phẩm đang khuyến mãi (Flash Sale)
    
    private Boolean isFeatured; // Sản phẩm nổi bật
    
    private Boolean isHighRating; // Sản phẩm đánh giá cao
    
    private String campaignSlug; // Slug của campaign promotion
    
    /**
     * Kiểu sắp xếp (enum):
     * - PRICE_ASC: Giá thấp đến cao
     * - PRICE_DESC: Giá cao đến thấp
     * - NAME_ASC: Tên A → Z
     * - NAME_DESC: Tên Z → A
     * - NEWEST: Mới nhất
     * - OLDEST: Cũ nhất
     * - RATING_ASC: Đánh giá thấp đến cao
     * - RATING_DESC: Đánh giá cao đến thấp
     */
    private ProductSortType sortBy;
    
    @Builder.Default
    private int page = 0; // Số trang (mặc định 0)
    
    @Builder.Default
    private int size = 8; // Số sản phẩm/trang (mặc định 8)
    
    /**
     * Chỉ lấy sản phẩm đang hoạt động (mặc định true)
     */
    @Builder.Default
    private Boolean isActive = true;
}

package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.MinimumRating;
import com.greenconnect.greenconnect_api.enums.PriceRange;
import com.greenconnect.greenconnect_api.enums.ProductSortType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho các tiêu chí lọc sản phẩm
 * Hỗ trợ lọc theo danh mục, giá, khuyến mãi, nhà cung cấp, từ khóa...
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {
    
    /**
     * Từ khóa tìm kiếm trong tên sản phẩm
     */
    private String keyword;
    
    /**
     * ID danh mục sản phẩm
     */
    private UUID categoryId;
    
    /**
     * Danh sách ID nhà cung cấp (hỗ trợ lọc nhiều suppliers cùng lúc)
     * Ví dụ: ?supplierIds=uuid1&supplierIds=uuid2&supplierIds=uuid3
     */
    private List<UUID> supplierIds;
    
    /**
     * Khoảng giá cố định (enum):
     * - ALL: Tất cả
     * - UNDER_100K: Dưới 100.000đ
     * - FROM_100K_TO_200K: 100.000đ - 200.000đ
     * - FROM_200K_TO_500K: 200.000đ - 500.000đ
     * - FROM_500K_TO_1M: 500.000đ - 1.000.000đ
     * - FROM_1M_TO_3M: 1.000.000đ - 3.000.000đ
     * - OVER_3M: Trên 3.000.000đ
     */
    private PriceRange priceRange;
    
    /**
     * Giá tối thiểu (tùy chỉnh - sử dụng khi không dùng priceRange)
     */
    private BigDecimal minPrice;
    
    /**
     * Giá tối đa (tùy chỉnh - sử dụng khi không dùng priceRange)
     */
    private BigDecimal maxPrice;
    
    /**
     * Lọc sản phẩm đang được khuyến mãi (có discountPrice)
     */
    private Boolean isOnSale;
    
    /**
     * Lọc sản phẩm nổi bật
     */
    private Boolean isFeatured;
    
    /**
     * Lọc sản phẩm có còn hàng (stockQuantity > 0)
     */
    private Boolean inStock;
    
    /**
     * Rating tối thiểu (enum):
     * - ALL: Tất cả
     * - ONE_STAR: 1 sao trở lên
     * - TWO_STAR: 2 sao trở lên
     * - THREE_STAR: 3 sao trở lên
     * - FOUR_STAR: 4 sao trở lên
     * - FIVE_STAR: 5 sao
     */
    private MinimumRating minRating;
    
    /**
     * Loại sắp xếp (enum):
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
    
    /**
     * Chỉ lấy sản phẩm đang hoạt động (mặc định true)
     */
    @Builder.Default
    private Boolean isActive = true;
}
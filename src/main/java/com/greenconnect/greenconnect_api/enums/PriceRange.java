package com.greenconnect.greenconnect_api.enums;

import java.math.BigDecimal;

import lombok.Getter;

/**
 * Enum cho các khoảng giá được định nghĩa sẵn
 * Dùng để lọc sản phẩm theo mức giá cố định
 */
@Getter
public enum PriceRange {
    ALL("Tất cả", null, null),
    UNDER_100K("Dưới 100.000đ", BigDecimal.ZERO, new BigDecimal("100000")),
    FROM_100K_TO_200K("100.000đ - 200.000đ", new BigDecimal("100000"), new BigDecimal("200000")),
    FROM_200K_TO_500K("200.000đ - 500.000đ", new BigDecimal("200000"), new BigDecimal("500000")),
    FROM_500K_TO_1M("500.000đ - 1.000.000đ", new BigDecimal("500000"), new BigDecimal("1000000")),
    FROM_1M_TO_3M("1.000.000đ - 3.000.000đ", new BigDecimal("1000000"), new BigDecimal("3000000")),
    OVER_3M("Trên 3.000.000đ", new BigDecimal("3000000"), null);

    private final String label;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;

    PriceRange(String label, BigDecimal minPrice, BigDecimal maxPrice) {
        this.label = label;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    /**
     * Kiểm tra giá có nằm trong khoảng này không
     */
    public boolean inRange(BigDecimal price) {
        if (price == null) return false;
        
        if (this == ALL) return true;
        
        boolean aboveMin = (minPrice == null) || (price.compareTo(minPrice) >= 0);
        boolean belowMax = (maxPrice == null) || (price.compareTo(maxPrice) < 0);
        
        return aboveMin && belowMax;
    }
}

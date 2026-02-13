package com.greenconnect.greenconnect_api.enums;

import lombok.Getter;

/**
 * Enum cho các kiểu sắp xếp sản phẩm
 */
@Getter
public enum ProductSortType {
    PRICE_ASC("Giá thấp đến cao"),
    PRICE_DESC("Giá cao đến thấp"),
    NAME_ASC("Tên A → Z"),
    NAME_DESC("Tên Z → A"),
    NEWEST("Mới nhất"),
    OLDEST("Cũ nhất"),
    RATING_ASC("Đánh giá thấp đến cao"),
    RATING_DESC("Đánh giá cao đến thấp");

    private final String label;

    ProductSortType(String label) {
        this.label = label;
    }
}

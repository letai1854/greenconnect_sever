package com.greenconnect.greenconnect_api.enums;

import java.math.BigDecimal;

import lombok.Getter;

/**
 * Enum cho các mức đánh giá tối thiểu
 */
@Getter
public enum MinimumRating {
    ALL("Tất cả", BigDecimal.ZERO),
    ONE_STAR("1 sao trở lên", BigDecimal.ONE),
    TWO_STAR("2 sao trở lên", new BigDecimal("2")),
    THREE_STAR("3 sao trở lên", new BigDecimal("3")),
    FOUR_STAR("4 sao trở lên", new BigDecimal("4")),
    FIVE_STAR("5 sao", new BigDecimal("5"));

    private final String label;
    private final BigDecimal value;

    MinimumRating(String label, BigDecimal value) {
        this.label = label;
        this.value = value;
    }
}

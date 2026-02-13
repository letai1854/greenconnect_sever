package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductRatingRequest {
    
    @NotNull(message = "Điểm đánh giá trung bình không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm đánh giá không được âm")
    @DecimalMax(value = "5.0", message = "Điểm đánh giá không được vượt quá 5.0")
    private BigDecimal averageRating;
    
    @NotNull(message = "Số lượng đánh giá không được để trống")
    @Min(value = 0, message = "Số lượng đánh giá không được âm")
    private Integer reviewCount;
}
package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho tìm kiếm danh mục với phân trang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCategoryRequest {
    
    /**
     * Từ khóa tìm kiếm (tiếng Việt có dấu)
     */
    @NotBlank(message = "Từ khóa tìm kiếm không được để trống")
    private String keyword;
    
    /**
     * Tab lọc theo trạng thái: active, inactive, all
     */
    @NotBlank(message = "Tab không được để trống")
    @Pattern(regexp = "^(active|inactive|all)$", message = "Tab chỉ có thể là: active, inactive, all")
    private String tab;
    
    /**
     * Số trang (bắt đầu từ 0)
     */
    @Min(value = 0, message = "Page phải >= 0")
    @Builder.Default
    private int page = 0;
    
    /**
     * Số lượng bản ghi mỗi trang
     */
    @Min(value = 1, message = "Size phải >= 1")
    @Builder.Default
    private int size = 20;
}

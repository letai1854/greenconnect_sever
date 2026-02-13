package com.greenconnect.greenconnect_api.dtos.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO cho danh sách users đã lọc.
 * <p>Wrapper chứa thông tin phân trang và danh sách users lọc.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FilterUsersResponse {
    
    /**
     * Danh sách users đã lọc.
     */
    List<UserResponse> users;
    
    /**
     * Tổng số lượng users tìm thấy.
     */
    Long totalElements;
    
    /**
     * Tổng số trang.
     */
    Integer totalPages;
    
    /**
     * Trang hiện tại (0-indexed).
     */
    Integer currentPage;
    
    /**
     * Số lượng records trên trang.
     */
    Integer pageSize;
}

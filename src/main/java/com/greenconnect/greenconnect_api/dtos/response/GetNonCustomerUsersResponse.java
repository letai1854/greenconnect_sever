package com.greenconnect.greenconnect_api.dtos.response;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO cho danh sách users không phải CUSTOMER role.
 * <p>Wrapper chứa thông tin phân trang và danh sách users.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetNonCustomerUsersResponse {
    
    /**
     * Danh sách users không phải CUSTOMER role.
     */
    List<UserResponse> users;
    
    /**
     * Tổng số lượng users.
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
    
    /**
     * Có phải trang cuối cùng không.
     */
    Boolean isLastPage;
    
    /**
     * Tạo response từ Page<UserResponse>.
     * @param page Page object từ repository
     * @param currentPageNum Trang hiện tại (0-indexed)
     * @return GetNonCustomerUsersResponse
     */
    public static GetNonCustomerUsersResponse fromPage(Page<UserResponse> page, Integer currentPageNum) {
        return GetNonCustomerUsersResponse.builder()
                .users(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(currentPageNum)
                .pageSize(page.getSize())
                .isLastPage(page.isLast())
                .build();
    }
}

package com.greenconnect.greenconnect_api.dtos.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để lấy danh sách khách hàng (CUSTOMER role).
 * <p>ADMIN và CUSTOMER_SUPPORT sử dụng endpoint này để xem danh sách khách hàng</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetCustomersRequest {
    
    /**
     * Trang (page number) - từ 0 trở đi.
     * <p>Giá trị mặc định: 0</p>
     */
    @Builder.Default
    Integer pageNumber = 0;
    
    /**
     * Số lượng records trên mỗi trang.
     * <p>Giá trị mặc định: 20</p>
     */
    @Builder.Default
    Integer pageSize = 20;
    
    /**
     * Trường để sắp xếp (sort field).
     * <p>Giá trị mặc định: createdAt</p>
     * <p>Các giá trị hợp lệ: id, email, fullName, createdAt, updatedAt</p>
     */
    @Builder.Default
    String sort = "createdAt";
    
    /**
     * Hướng sắp xếp (ASC hoặc DESC).
     * <p>Giá trị mặc định: DESC (giảm dần, mới nhất trước)</p>
     */
    @Builder.Default
    String sortDirection = "DESC";
}

package com.greenconnect.greenconnect_api.dtos.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO để lọc danh sách users với các tiêu chí nâng cao.
 * <p>Admin sử dụng endpoint này để lọc users theo: tên/email/điện thoại, trạng thái, vai trò, ngày tạo</p>
 * <p>✅ TẤT CẢ CÁC TRƯỜNG ĐỀU OPTIONAL - Để trống để lấy tất cả</p>
 * <p>⚠️ DỮ LIỆU NHẬN VÀO LÀ TIẾNG ANH (ENUM VALUES)</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FilterUsersRequest {
    
    /**
     * Từ khóa tìm kiếm trong tên (fullName), email, hoặc số điện thoại (phoneNumber).
     * <p>Tìm kiếm không phân biệt hoa thường với LIKE %keyword%</p>
     * <p>✅ Optional: null hoặc rỗng = không tìm kiếm, lấy tất cả</p>
     * <p>Ví dụ: "Trần" sẽ tìm trong fullName, email, phoneNumber có chứa "trần"</p>
     */
    String searchField;
    
    /**
     * Trạng thái tài khoản (ENUM tiếng Anh).
     * <p>Giá trị cho phép: "ACTIVE", "INACTIVE", "PENDING_ACTIVATION", "BANNED"</p>
     * <p>✅ Optional: null hoặc rỗng = lấy tất cả trạng thái</p>
     */
    String status;
    
    /**
     * Danh sách vai trò (ENUM tiếng Anh) cần lọc (Set các giá trị).
     * <p>Ví dụ: ["CUSTOMER", "ADMIN", "ORDER_MANAGER", "PRODUCT_MANAGER"]</p>
     * <p>Giá trị cho phép: CUSTOMER, ADMIN, ORDER_MANAGER, PRODUCT_MANAGER, MARKETING_MANAGER, CUSTOMER_SUPPORT, SHIPPER</p>
     * <p>✅ Optional: null hoặc empty = không lọc theo vai trò, lấy tất cả</p>
     */
    List<String> roles;
    
    /**
     * Ngày bắt đầu tạo tài khoản (inclusive).
     * <p>Format: yyyy-MM-dd (ví dụ: "2025-01-01")</p>
     * <p>✅ Optional: null = không giới hạn ngày bắt đầu</p>
     */
    LocalDate startDate;
    
    /**
     * Ngày kết thúc tạo tài khoản (inclusive).
     * <p>Format: yyyy-MM-dd (ví dụ: "2025-12-31")</p>
     * <p>✅ Optional: null = không giới hạn ngày kết thúc</p>
     */
    LocalDate endDate;
    
    /**
     * Số trang (page number) - bắt đầu từ 0.
     * <p>✅ Optional: null = sử dụng mặc định 0 (trang đầu tiên)</p>
     * <p>Giá trị mặc định: 0</p>
     */
    @Builder.Default
    @Min(value = 0, message = "Page number phải >= 0")
    Integer pageNumber = 0;
    
    /**
     * Số lượng records trên mỗi trang.
     * <p>✅ Optional: null = sử dụng mặc định 20</p>
     * <p>Giá trị mặc định: 20</p>
     */
    @Builder.Default
    @Min(value = 1, message = "Page size phải >= 1")
    Integer pageSize = 20;
    
    /**
     * Trường để sắp xếp (sort field).
     * <p>Các giá trị hợp lệ: id, email, fullName, createdAt, updatedAt, status</p>
     * <p>✅ Optional: null hoặc rỗng = sử dụng mặc định "createdAt"</p>
     * <p>Giá trị mặc định: createdAt</p>
     */
    @Builder.Default
    String sortBy = "createdAt";
    
    /**
     * Hướng sắp xếp (ASC hoặc DESC).
     * <p>✅ Optional: null hoặc rỗng = sử dụng mặc định "DESC"</p>
     * <p>Giá trị mặc định: DESC (giảm dần, mới nhất trước)</p>
     */
    @Builder.Default
    String sortDirection = "DESC";
}

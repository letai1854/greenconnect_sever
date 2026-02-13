package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.VoucherCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.VoucherUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.VoucherResponse;
import com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserVoucherResponse;

public interface VoucherService {
    
    /**
     * Tạo voucher mới (Admin only)
     */
    VoucherResponse createVoucher(VoucherCreateRequest request);
    
    /**
     * Cập nhật voucher (Admin only)
     */
    VoucherResponse updateVoucher(UUID voucherId, VoucherUpdateRequest request);
    
    /**
     * Lấy chi tiết voucher theo ID
     */
    VoucherResponse getVoucherById(UUID voucherId);
    
    /**
     * Lấy danh sách voucher với phân trang và tìm kiếm (Admin)
     */
    Page<VoucherResponse> getAllVouchers(String search, Boolean isActive, Pageable pageable);
    
    /**
     * Lấy danh sách voucher còn hạn và có thể sử dụng (Public)
     */
    Page<VoucherResponse> getAvailableVouchers(Pageable pageable);
    
    /**
     * Tìm voucher theo code để áp dụng (dùng trong Order)
     */
    VoucherResponse findByVoucherCode(String voucherCode);
    
    /**
     * Kích hoạt/vô hiệu hóa voucher
     */
    VoucherResponse toggleVoucherStatus(UUID voucherId);
    
    /**
     * Lấy tất cả voucher đang active (không phân trang)
     */
    List<VoucherResponse> getAllActiveVouchers();
    
    /**
     * Lấy danh sách voucher cho user với trạng thái khác nhau (có thể dùng, đã hết hạn, đã dùng...)
     */
    Page<UserVoucherResponse> getUserVouchers(UUID userId, Pageable pageable);
    
    /**
     * Xóa voucher (soft delete - chuyển isActive = false)
     */
    void deleteVoucher(UUID voucherId);
    
    /**
     * Lấy danh sách voucher active với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<VoucherResponse> getActiveVouchersPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy danh sách voucher inactive với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<VoucherResponse> getInactiveVouchersPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy tất cả voucher với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<VoucherResponse> getAllVouchersPaginatedByCreatedAt(int page, int size);
    
    /**
     * Tìm kiếm voucher với phân trang theo từ khóa và tab (active/inactive/all)
     */
    Page<VoucherResponse> searchVouchers(String keyword, String tab, int page, int size);
    
    /**
     * Lấy danh sách voucher active kèm theo trạng thái sử dụng của user
     * 
     * @param userId ID của user
     * @param pageable Phân trang
     * @return Page<VoucherWithUsageResponse> - Danh sách voucher với thông tin đã dùng chưa
     */
    Page<VoucherWithUsageResponse> getActiveVouchersWithUserUsage(UUID userId, Pageable pageable);
}
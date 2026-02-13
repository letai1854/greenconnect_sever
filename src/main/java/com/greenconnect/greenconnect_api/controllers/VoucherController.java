package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.VoucherCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.VoucherUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.UserVoucherResponse;
import com.greenconnect.greenconnect_api.dtos.response.VoucherResponse;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.VoucherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller để quản lý voucher/mã giảm giá
 * 
 * Endpoints:
 * - ADMIN: Tạo, sửa, xóa, quản lý voucher
 * - PUBLIC: Xem danh sách voucher còn hạn
 * - INTERNAL: Tìm voucher theo code (dùng trong OrderService)
 */
@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {
    
    private final VoucherService voucherService;

    /**
     * Tạo voucher mới (Admin + Marketing Manager)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> createVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        log.info("Tạo voucher mới với code: {}", request.getVoucherCode());
        
        VoucherResponse response = voucherService.createVoucher(request);
        ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
            response, 
            "Tạo voucher thành công"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Cập nhật voucher (Admin + Marketing Manager)
     */
    @PutMapping("/update/{voucherId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateVoucher(
            @PathVariable UUID voucherId,
            @Valid @RequestBody VoucherUpdateRequest request) {
        log.info("Cập nhật voucher với ID: {}", voucherId);
        
        VoucherResponse response = voucherService.updateVoucher(voucherId, request);
        ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
            response, 
            "Cập nhật voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy chi tiết voucher theo ID (Admin + Marketing Manager)
     */
    @GetMapping("/{voucherId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucherById(@PathVariable UUID voucherId) {
        log.info("Lấy chi tiết voucher với ID: {}", voucherId);
        
        VoucherResponse response = voucherService.getVoucherById(voucherId);
        ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
            response, 
            "Lấy thông tin voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy danh sách voucher với phân trang và tìm kiếm (Admin + Marketing Manager)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAllVouchers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Lấy danh sách voucher - search: '{}', isActive: {}, page: {}, size: {}", 
                search, isActive, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VoucherResponse> response = voucherService.getAllVouchers(search, isActive, pageable);
        
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy danh sách voucher có thể sử dụng (Public)
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAvailableVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Lấy danh sách voucher có thể sử dụng - page: {}, size: {}", page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VoucherResponse> response = voucherService.getAvailableVouchers(pageable);
        
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher có thể sử dụng thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Tìm voucher theo code (Internal API - dùng trong OrderService)
     * <p>Admin + Marketing Manager có thể tìm kiếm voucher theo code</p>
     */
    @GetMapping("/find-by-code/{voucherCode}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> findByVoucherCode(@PathVariable String voucherCode) {
        log.info("Tìm voucher theo code: {}", voucherCode);
        
        VoucherResponse response = voucherService.findByVoucherCode(voucherCode);
        ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
            response, 
            "Tìm voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Kích hoạt/vô hiệu hóa voucher (Admin only)
     */
    @PatchMapping("/toggle-status/{voucherId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VOUCHER_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> toggleVoucherStatus(@PathVariable UUID voucherId) {
        log.info("Thay đổi trạng thái voucher với ID: {}", voucherId);
        
        VoucherResponse response = voucherService.toggleVoucherStatus(voucherId);
        ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
            response, 
            "Thay đổi trạng thái voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Xóa voucher (soft delete - Admin only)
     */
    @DeleteMapping("/{voucherId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VOUCHER_MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteVoucher(@PathVariable UUID voucherId) {
        log.info("Xóa voucher với ID: {}", voucherId);
        
        voucherService.deleteVoucher(voucherId);
        ApiResponse<String> apiResponse = ResponseUtil.success(
            "Voucher đã được xóa", 
            "Xóa voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy tất cả voucher đang active (Public API - không phân trang)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getAllActiveVouchers() {
        log.info("Lấy tất cả voucher đang active");
        
        List<VoucherResponse> response = voucherService.getAllActiveVouchers();
        ApiResponse<List<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher active thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Validate voucher code (Public API - để check voucher trước khi apply)
     */
    @GetMapping("/validate/{voucherCode}")
    public ResponseEntity<ApiResponse<VoucherResponse>> validateVoucherCode(@PathVariable String voucherCode) {
        log.info("Validate voucher code: {}", voucherCode);
        
        try {
            VoucherResponse response = voucherService.findByVoucherCode(voucherCode);
            
            if (!response.getIsAvailable()) {
                ApiResponse<VoucherResponse> apiResponse = ResponseUtil.error(
                    ErrorCode.VOUCHER_NOT_AVAILABLE, 
                    "Voucher không khả dụng hoặc đã hết hạn"
                );
                return ResponseEntity.badRequest().body(apiResponse);
            }
            
            ApiResponse<VoucherResponse> apiResponse = ResponseUtil.success(
                response, 
                "Voucher hợp lệ và có thể sử dụng"
            );
            
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<VoucherResponse> apiResponse = ResponseUtil.error(
                ErrorCode.VOUCHER_NOT_FOUND, 
                "Không tìm thấy voucher với mã: " + voucherCode
            );
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    /**
     * Lấy danh sách voucher dành cho user (có thể dùng, đã dùng, hết hạn...)
     * User endpoint - hiển thị voucher theo trạng thái phù hợp
     * Sắp xếp: Voucher còn dùng được hiển thị trước, sau đó sắp xếp theo thời gian tạo giảm dần
     */
    @GetMapping("/user-vouchers")
    public ResponseEntity<ApiResponse<Page<UserVoucherResponse>>> getUserVouchers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        log.info("Lấy danh sách voucher cho user");
        
        // Lấy userId từ JWT token
        UUID userId = getCurrentUserId();
        
        // Sort: Voucher còn dùng được (isAvailable) trước, sau đó theo createdDate giảm dần
        Sort sort = Sort.by(
            Sort.Order.desc("isAvailable"),  // Voucher còn dùng được hiển thị trước
            Sort.Order.desc("createdDate")   // Sau đó sắp xếp theo ngày tạo mới nhất
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserVoucherResponse> response = voucherService.getUserVouchers(userId, pageable);
        
        ApiResponse<Page<UserVoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher cho user thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    private UUID getCurrentUserId() {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userPrincipal.getUserId();
    }

    /**
     * Lấy danh sách voucher active với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/active/latest/page")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getActiveVouchersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy danh sách voucher active với phân trang - page: {}, size: {}", page, size);
        
        Page<VoucherResponse> response = voucherService.getActiveVouchersPaginatedByCreatedAt(page, size);
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher active thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy danh sách voucher inactive với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/inactive/latest/page")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getInactiveVouchersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách voucher inactive với phân trang - page: {}, size: {}", page, size);
        
        Page<VoucherResponse> response = voucherService.getInactiveVouchersPaginatedByCreatedAt(page, size);
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher inactive thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy tất cả voucher với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/all/latest/page")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAllVouchersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả voucher với phân trang - page: {}, size: {}", page, size);
        
        Page<VoucherResponse> response = voucherService.getAllVouchersPaginatedByCreatedAt(page, size);
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách voucher thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Tìm kiếm voucher với phân trang - Hỗ trợ tiếng Việt có dấu
     * @param keyword Từ khóa tìm kiếm (tiếng Việt có dấu)
     * @param tab Trạng thái: active, inactive, all
     * @param page Số trang (mặc định 0)
     * @param size Số lượng/trang (mặc định 20)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> searchVouchers(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔍 Tìm kiếm voucher - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        // Validate tab
        if (!tab.matches("^(active|inactive|all)$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        Page<VoucherResponse> response = voucherService.searchVouchers(keyword, tab, page, size);
        ApiResponse<Page<VoucherResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Tìm kiếm voucher thành công - Tìm thấy " + response.getTotalElements() + " kết quả"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy danh sách voucher active kèm theo trạng thái sử dụng của user
     * <p>Public endpoint - Yêu cầu login</p>
     * 
     * <p><strong>Mục đích:</strong> Hiển thị danh sách voucher cho user xem, 
     * mỗi voucher sẽ có thông tin user đã dùng bao nhiêu lần, còn dùng được không</p>
     * 
     * @param userId ID của user (từ path variable)
     * @param page Trang hiện tại (default: 0)
     * @param size Số lượng voucher mỗi trang (default: 20)
     * @return Page<VoucherWithUsageResponse> - Danh sách voucher với trạng thái sử dụng
     * 
     * Example response:
     * {
     *   "code": 200,
     *   "message": "Lấy danh sách voucher thành công",
     *   "data": {
     *     "content": [
     *       {
     *         "id": "uuid",
     *         "voucher_code": "FREESHIP50K",
     *         "discount_value": 50000,
     *         "user_used_count": 1,
     *         "is_fully_used_by_user": true,
     *         "is_available": false
     *       }
     *     ]
     *   }
     * }
     */
    @GetMapping("/user/{userId}/with-usage")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse>>> getVouchersWithUserUsage(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📋 GET /vouchers/user/{}/with-usage - Lấy voucher với trạng thái sử dụng (page: {}, size: {})", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse> response = 
            voucherService.getActiveVouchersWithUserUsage(userId, pageable);
        
        String message = String.format(
            "Lấy danh sách %d voucher thành công (trang %d/%d)", 
            response.getNumberOfElements(),
            response.getNumber() + 1,
            response.getTotalPages()
        );
        
        ApiResponse<Page<com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse>> apiResponse = 
            ResponseUtil.success(response, message);
        
        return ResponseEntity.ok(apiResponse);
    }
}

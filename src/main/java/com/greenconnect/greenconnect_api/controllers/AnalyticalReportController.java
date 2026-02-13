package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dto.reports.analytical.AnalyticalReportResponse;
import com.greenconnect.greenconnect_api.dto.reports.analytical.TabType;
import com.greenconnect.greenconnect_api.dto.reports.analytical.TimePreset;
import com.greenconnect.greenconnect_api.services.AnalyticalReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller cho Analytical Report API
 * Endpoint chính: GET /api/reports/analytical
 * 
 * Features:
 * - Hỗ trợ 3 tabs: OVERVIEW (tổng quan tài chính), PRODUCT (sản phẩm), CUSTOMER (khách hàng)
 * - Hỗ trợ custom date range với startDate & endDate
 * - Caching 30 phút cho mỗi tab
 * - Rate limiting thông báo client
 * 
 * Authorization:
 * - ADMIN: Full access to all tabs
 * - QUAN_LY_DON_HANG: Access to OVERVIEW and PRODUCT tabs
 * - MARKETING: Access to all tabs
 * 
 * Response Codes:
 * - 200: Success
 * - 400: Invalid parameters (missing dates, invalid tab, etc.)
 * - 403: Forbidden (role không có quyền)
 * - 429: Too Many Requests (rate limit - đã request trong 30 phút)
 */
@RestController
@RequestMapping("/api/reports/analytical")
@RequiredArgsConstructor
@Slf4j
public class AnalyticalReportController {

    private final AnalyticalReportService analyticalReportService;

    /**
     * Lấy báo cáo phân tích theo tab
     * 
     * GET /api/reports/analytical?tab=OVERVIEW&preset=THIRTY_DAYS
     * GET /api/reports/analytical?tab=PRODUCT&preset=CUSTOM&startDate=2024-01-01&endDate=2024-12-31
     * 
     * Query Parameters:
     * - tab: OVERVIEW | PRODUCT | CUSTOMER (required)
     * - preset: SEVEN_DAYS | THIRTY_DAYS | NINETY_DAYS | THIS_MONTH | LAST_MONTH | THIS_YEAR | CUSTOM (optional, default: THIRTY_DAYS)
     * - startDate: yyyy-MM-dd format (required khi preset=CUSTOM)
     * - endDate: yyyy-MM-dd format (required khi preset=CUSTOM)
     * 
     * Các mốc thời gian cố định:
     * - SEVEN_DAYS: 7 ngày gần nhất
     * - THIRTY_DAYS: 30 ngày gần nhất
     * - NINETY_DAYS: 90 ngày gần nhất
     * - THIS_MONTH: Tháng hiện tại (từ ngày 1 đến hôm nay)
     * - LAST_MONTH: Tháng trước (toàn bộ tháng)
     * - THIS_YEAR: Năm hiện tại (từ 1/1 đến hôm nay)
     * - CUSTOM: Tùy chọn startDate & endDate
     * 
     * Authorization:
     * - ADMIN: Full access to all tabs
     * - QUAN_LY_DON_HANG: OVERVIEW and PRODUCT only
     * - MARKETING: Full access to all tabs
     * 
     * Responses:
     * - 200: Success with AnalyticalReportResponse
     * - 400: Invalid parameters (bad tab, missing dates for CUSTOM preset)
     * - 403: Forbidden (role không đủ quyền)
     * - 429: Rate limit hit (đã request trong 30 phút gần đây)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT', 'MARKETING')")
    public ResponseEntity<AnalyticalReportResponse> getAnalyticalReport(
            @RequestParam TabType tab,
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("📊 [AnalyticalReport] Request received - tab: {}, preset: {}, startDate: {}, endDate: {}", 
                tab, preset, startDate, endDate);
        
        // Parse preset string to enum (client gửi "SEVEN_DAYS", "THIRTY_DAYS", etc.)
        TimePreset timePreset = TimePreset.fromValue(preset);
        
        // Validate CUSTOM preset requires dates
        if (timePreset == TimePreset.CUSTOM && (startDate == null || endDate == null)) {
            log.error("❌ [AnalyticalReport] CUSTOM preset requires startDate and endDate");
            throw new IllegalArgumentException("Preset CUSTOM yêu cầu cung cấp startDate và endDate");
        }
        
        AnalyticalReportResponse response = analyticalReportService.getAnalyticalReport(
                preset, 
                startDate, 
                endDate, 
                tab
        );
        
        log.info("✅ [AnalyticalReport] Report generated successfully for tab: {}, preset: {}", tab, timePreset);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách VIP customers với phân trang
     * 
     * GET /api/reports/analytical/vip-customers?preset=THIRTY_DAYS&page=0&size=10
     * GET /api/reports/analytical/vip-customers?preset=CUSTOM&startDate=2024-01-01&endDate=2024-12-31&page=1&size=10
     * 
     * Query Parameters:
     * - preset: SEVEN_DAYS | THIRTY_DAYS | NINETY_DAYS | THIS_MONTH | LAST_MONTH | THIS_YEAR | CUSTOM (optional, default: THIRTY_DAYS)
     * - startDate: yyyy-MM-dd format (required khi preset=CUSTOM)
     * - endDate: yyyy-MM-dd format (required khi preset=CUSTOM)
     * - page: Số trang (0-indexed, mặc định 0)
     * - size: Kích thước trang (mặc định 10)
     * 
     * Sort: Tự động sắp xếp theo totalSpent DESC (giữ nguyên logic hiện tại)
     * 
     * Authorization:
     * - ADMIN: Full access
     * - QUAN_LY_DON_HANG: Access to customer data
     * - MARKETING: Full access
     * 
     * Responses:
     * - 200: Success with Page<VipCustomerData>
     * - 400: Invalid parameters (bad preset, missing dates for CUSTOM)
     * - 403: Forbidden (role không đủ quyền)
     */
    @GetMapping("/vip-customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SUPPORT', 'MARKETING')")
    public ResponseEntity<org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dto.reports.analytical.CustomerResponse.VipCustomerData>> getVipCustomersPaginated(
            @RequestParam(required = false, defaultValue = "THIRTY_DAYS") String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📊 [VipCustomers] Request received - preset: {}, startDate: {}, endDate: {}, page: {}, size: {}", 
                preset, startDate, endDate, page, size);
        
        // Parse preset string to enum
        TimePreset timePreset = TimePreset.fromValue(preset);
        
        // Validate CUSTOM preset requires dates
        if (timePreset == TimePreset.CUSTOM && (startDate == null || endDate == null)) {
            log.error("❌ [VipCustomers] CUSTOM preset requires startDate and endDate");
            throw new IllegalArgumentException("Preset CUSTOM yêu cầu cung cấp startDate và endDate");
        }
        
        // Calculate date range based on preset
        LocalDate[] dateRange = calculateDateRange(timePreset, startDate, endDate);
        LocalDate finalStartDate = dateRange[0];
        LocalDate finalEndDate = dateRange[1];
        
        log.info("📅 [VipCustomers] Date range: {} to {}", finalStartDate, finalEndDate);
        
        org.springframework.data.domain.Page<com.greenconnect.greenconnect_api.dto.reports.analytical.CustomerResponse.VipCustomerData> response = 
            analyticalReportService.getVipCustomersPaginated(finalStartDate, finalEndDate, page, size);
        
        log.info("✅ [VipCustomers] Retrieved {} VIP customers (page {}/{}, total: {})", 
                response.getNumberOfElements(), page + 1, response.getTotalPages(), response.getTotalElements());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method: Tính toán date range dựa trên preset (theo logic cũ)
     */
    private LocalDate[] calculateDateRange(TimePreset preset, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end = today;
        
        switch (preset) {
            case SEVEN_DAYS:
                start = today.minusDays(7);
                break;
            case THIRTY_DAYS:
                start = today.minusDays(30);
                break;
            case NINETY_DAYS:
                start = today.minusDays(90);
                break;
            case THIS_YEAR:
                start = today.withDayOfYear(1);
                break;
            case CUSTOM:
                start = startDate;
                end = endDate;
                break;
            default:
                start = today.minusDays(30); // Default 30 days
                break;
        }
        
        return new LocalDate[]{start, end};
    }

}

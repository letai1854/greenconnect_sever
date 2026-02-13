package com.greenconnect.greenconnect_api.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.response.report.BusinessTargetsResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomerRetentionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomersByRegionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.MonthlyRevenueComparisonResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.OrdersHeatmapResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.VipCustomersResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.WeeklyTrendsResponse;
import com.greenconnect.greenconnect_api.services.ReportService;

import lombok.RequiredArgsConstructor;

/**
 * Report Controller - Advanced Analytics & Charts
 * 
 * Các endpoint phục vụ biểu đồ và phân tích nâng cao:
 * 1. So sánh doanh thu theo năm (Monthly Revenue Comparison)
 * 2. Phân bổ khách hàng theo khu vực (Customers by Region)
 * 3. Tỷ lệ khách hàng quay lại (Customer Retention)
 * 4. Mục tiêu kinh doanh (Business Targets)
 * 5. Heatmap đơn hàng theo giờ/ngày (Orders Heatmap)
 * 6. Khách hàng VIP (VIP Customers)
 * 7. Xu hướng tuần (Weekly Trends)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * 1. So sánh doanh thu theo tháng giữa 2 năm
     * 
     * GET /api/reports/monthly-revenue-comparison?year=2025&compareYear=2024
     * 
     * @param year        Năm hiện tại cần xem (mặc định: năm nay)
     * @param compareYear Năm so sánh (mặc định: năm trước)
     * @return Dữ liệu doanh thu 12 tháng của cả 2 năm + tỷ lệ tăng trưởng
     */
    @GetMapping("/monthly-revenue-comparison")
    public ResponseEntity<MonthlyRevenueComparisonResponse> getMonthlyRevenueComparison(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @RequestParam(required = false) Integer compareYear
    ) {
        return ResponseEntity.ok(reportService.getMonthlyRevenueComparison(year, compareYear));
    }

    /**
     * 2. Phân bổ khách hàng theo khu vực (tỉnh/thành)
     * 
     * GET /api/reports/customers-by-region
     * GET /api/reports/customers-by-region?startDate=2025-01-01&endDate=2025-01-31
     * 
     * @param startDate Ngày bắt đầu (optional)
     * @param endDate   Ngày kết thúc (optional)
     * @return Top 5 tỉnh/thành + Khác
     */
    @GetMapping("/customers-by-region")
    public ResponseEntity<CustomersByRegionResponse> getCustomersByRegion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getCustomersByRegion(startDate, endDate));
    }

    /**
     * 3. Tỷ lệ khách hàng quay lại vs khách mới
     * 
     * GET /api/reports/customer-retention
     * GET /api/reports/customer-retention?startDate=2025-01-01&endDate=2025-01-31
     * 
     * @param startDate Ngày bắt đầu (mặc định: 30 ngày trước)
     * @param endDate   Ngày kết thúc (mặc định: hôm nay)
     * @return Tỷ lệ % và số lượng khách quay lại, khách mới
     */
    @GetMapping("/customer-retention")
    public ResponseEntity<CustomerRetentionResponse> getCustomerRetention(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getCustomerRetention(startDate, endDate));
    }

    /**
     * 4. Mục tiêu kinh doanh tháng
     * 
     * GET /api/reports/business-targets
     * GET /api/reports/business-targets?month=1&year=2025
     * 
     * @param month Tháng (mặc định: tháng hiện tại)
     * @param year  Năm (mặc định: năm hiện tại)
     * @return KPI targets với current/target/percentage
     */
    @GetMapping("/business-targets")
    public ResponseEntity<BusinessTargetsResponse> getBusinessTargets(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year
    ) {
        return ResponseEntity.ok(reportService.getBusinessTargets(month, year));
    }

    /**
     * 5. Heatmap đơn hàng theo giờ và ngày trong tuần
     * 
     * GET /api/reports/orders-heatmap
     * GET /api/reports/orders-heatmap?startDate=2025-01-01&endDate=2025-01-31
     * 
     * @param startDate Ngày bắt đầu (mặc định: 30 ngày trước)
     * @param endDate   Ngày kết thúc (mặc định: hôm nay)
     * @return Ma trận 7 ngày x 24 giờ với số đơn hàng
     */
    @GetMapping("/orders-heatmap")
    public ResponseEntity<OrdersHeatmapResponse> getOrdersHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getOrdersHeatmap(startDate, endDate));
    }

    /**
     * 6. Danh sách khách hàng VIP (chi tiêu cao nhất)
     * 
     * GET /api/reports/vip-customers
     * GET /api/reports/vip-customers?limit=10&sortBy=totalSpent
     * GET /api/reports/vip-customers?limit=5&sortBy=orderCount&startDate=2025-01-01&endDate=2025-12-31
     * 
     * @param limit     Số lượng khách hàng (mặc định: 10)
     * @param startDate Ngày bắt đầu (mặc định: 1 năm trước)
     * @param endDate   Ngày kết thúc (mặc định: hôm nay)
     * @param sortBy    Sắp xếp theo: totalSpent (mặc định), orderCount, rating
     * @return Danh sách VIP customers với thông tin chi tiết
     */
    @GetMapping("/vip-customers")
    public ResponseEntity<VipCustomersResponse> getVipCustomers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "totalSpent") String sortBy
    ) {
        return ResponseEntity.ok(reportService.getVipCustomers(limit, startDate, endDate, sortBy));
    }

    /**
     * 7. Xu hướng 7 ngày gần nhất (Sparkline data)
     * 
     * GET /api/reports/weekly-trends
     * GET /api/reports/weekly-trends?endDate=2025-01-31
     * 
     * @param endDate Ngày kết thúc (mặc định: hôm nay), sẽ lấy 7 ngày từ endDate-6 đến endDate
     * @return Dữ liệu sparkline cho orders, revenue, new customers, rating
     */
    @GetMapping("/weekly-trends")
    public ResponseEntity<WeeklyTrendsResponse> getWeeklyTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getWeeklyTrends(endDate));
    }
}

package com.greenconnect.greenconnect_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.response.dashboard.ComparisonStatsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.OrderStatusChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.RevenueChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopCategoriesResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopProductsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopSuppliersResponse;
import com.greenconnect.greenconnect_api.services.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dashboard API Controller - API quản lý Dashboard Admin
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 📊 Endpoint 1: Thống kê so sánh tuần này vs tuần trước
     * - Doanh thu
     * - Số đơn hàng  
     * - Khách hàng mới
     * - Đánh giá trung bình
     */
    @GetMapping("/comparison-stats")
    public ResponseEntity<ComparisonStatsResponse> getComparisonStats() {
        log.info("📊 [Dashboard API] GET /comparison-stats");
        ComparisonStatsResponse response = dashboardService.getComparisonStats();
        return ResponseEntity.ok(response);
    }

    /**
     * 📈 Endpoint 2: Biểu đồ doanh thu theo ngày
     * - Doanh thu mỗi ngày trong N ngày gần nhất
     * - Số đơn hàng mỗi ngày
     */
    @GetMapping("/revenue-chart")
    public ResponseEntity<RevenueChartResponse> getRevenueChart(
            @RequestParam(defaultValue = "30") int days) {
        log.info("📈 [Dashboard API] GET /revenue-chart?days={}", days);
        
        // Validate days parameter
        if (days < 1) days = 7;
        if (days > 365) days = 365;
        
        RevenueChartResponse response = dashboardService.getRevenueChart(days);
        return ResponseEntity.ok(response);
    }

    /**
     * 🏆 Endpoint 3: Top sản phẩm bán chạy
     * - Tên sản phẩm
     * - Số lượng đã bán
     * - Doanh thu
     */
    @GetMapping("/top-products")
    public ResponseEntity<TopProductsResponse> getTopProducts(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("🏆 [Dashboard API] GET /top-products?limit={}", limit);
        
        // Validate limit parameter
        if (limit < 1) limit = 5;
        if (limit > 50) limit = 50;
        
        TopProductsResponse response = dashboardService.getTopProducts(limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 🥧 Endpoint 4: Biểu đồ phân bố trạng thái đơn hàng
     * - Số lượng và phần trăm mỗi trạng thái
     */
    @GetMapping("/order-status-chart")
    public ResponseEntity<OrderStatusChartResponse> getOrderStatusChart() {
        log.info("🥧 [Dashboard API] GET /order-status-chart");
        OrderStatusChartResponse response = dashboardService.getOrderStatusChart();
        return ResponseEntity.ok(response);
    }

    /**
     * 📁 Endpoint 5: Top danh mục bán chạy
     * - Tên danh mục
     * - Số sản phẩm
     * - Doanh thu
     */
    @GetMapping("/top-categories")
    public ResponseEntity<TopCategoriesResponse> getTopCategories(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("📁 [Dashboard API] GET /top-categories?limit={}", limit);
        
        // Validate limit parameter
        if (limit < 1) limit = 5;
        if (limit > 50) limit = 50;
        
        TopCategoriesResponse response = dashboardService.getTopCategories(limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 🏢 Endpoint 6: Top nhà cung cấp
     * - Tên NCC
     * - Số sản phẩm
     * - Doanh thu
     */
    @GetMapping("/top-suppliers")
    public ResponseEntity<TopSuppliersResponse> getTopSuppliers(
            @RequestParam(defaultValue = "5") int limit) {
        log.info("🏢 [Dashboard API] GET /top-suppliers?limit={}", limit);
        
        // Validate limit parameter
        if (limit < 1) limit = 5;
        if (limit > 50) limit = 50;
        
        TopSuppliersResponse response = dashboardService.getTopSuppliers(limit);
        return ResponseEntity.ok(response);
    }
}

package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.response.dashboard.ComparisonStatsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.OrderStatusChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.RevenueChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopCategoriesResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopProductsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopSuppliersResponse;

/**
 * Service interface cho Dashboard Statistics
 * Chia nhỏ các endpoint để tối ưu performance
 */
public interface DashboardService {
    
    /**
     * Lấy thống kê so sánh giữa tuần này và tuần trước
     * - Doanh thu
     * - Số đơn hàng
     * - Khách hàng mới
     * - Đánh giá trung bình
     */
    ComparisonStatsResponse getComparisonStats();
    
    /**
     * Lấy dữ liệu biểu đồ doanh thu 30 ngày gần nhất
     */
    RevenueChartResponse getRevenueChart(int days);
    
    /**
     * Lấy Top 5 sản phẩm bán chạy nhất (trong 30 ngày)
     */
    TopProductsResponse getTopProducts(int limit);
    
    /**
     * Lấy thống kê phân bố trạng thái đơn hàng
     */
    OrderStatusChartResponse getOrderStatusChart();
    
    /**
     * Lấy Top 5 danh mục có doanh số cao nhất (trong 30 ngày)
     */
    TopCategoriesResponse getTopCategories(int limit);
    
    /**
     * Lấy Top 5 nhà cung cấp có doanh số cao nhất (trong 30 ngày)
     */
    TopSuppliersResponse getTopSuppliers(int limit);
}

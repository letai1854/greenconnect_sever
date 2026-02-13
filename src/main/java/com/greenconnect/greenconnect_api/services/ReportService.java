package com.greenconnect.greenconnect_api.services;

import java.time.LocalDate;

import com.greenconnect.greenconnect_api.dtos.response.report.BusinessTargetsResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomerRetentionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomersByRegionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.MonthlyRevenueComparisonResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.OrdersHeatmapResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.VipCustomersResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.WeeklyTrendsResponse;

public interface ReportService {
    
    /**
     * So sánh doanh thu theo tháng giữa năm hiện tại và năm trước
     */
    MonthlyRevenueComparisonResponse getMonthlyRevenueComparison(int year, Integer compareYear);
    
    /**
     * Phân bổ khách hàng theo khu vực (tỉnh/thành phố)
     */
    CustomersByRegionResponse getCustomersByRegion(LocalDate startDate, LocalDate endDate);
    
    /**
     * Tỷ lệ khách hàng quay lại vs khách mới
     */
    CustomerRetentionResponse getCustomerRetention(LocalDate startDate, LocalDate endDate);
    
    /**
     * Mục tiêu kinh doanh tháng
     */
    BusinessTargetsResponse getBusinessTargets(int month, int year);
    
    /**
     * Heatmap đơn hàng theo giờ/ngày trong tuần
     */
    OrdersHeatmapResponse getOrdersHeatmap(LocalDate startDate, LocalDate endDate);
    
    /**
     * Top khách hàng VIP (chi tiêu nhiều nhất)
     */
    VipCustomersResponse getVipCustomers(int limit, LocalDate startDate, LocalDate endDate, String sortBy);
    
    /**
     * Xu hướng 7 ngày gần nhất
     */
    WeeklyTrendsResponse getWeeklyTrends(LocalDate endDate);
}

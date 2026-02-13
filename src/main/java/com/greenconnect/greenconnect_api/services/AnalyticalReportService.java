package com.greenconnect.greenconnect_api.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.greenconnect.greenconnect_api.dto.reports.analytical.AnalyticalReportResponse;
import com.greenconnect.greenconnect_api.dto.reports.analytical.CustomerResponse;
import com.greenconnect.greenconnect_api.dto.reports.analytical.OverviewResponse;
import com.greenconnect.greenconnect_api.dto.reports.analytical.ProductResponse;
import com.greenconnect.greenconnect_api.dto.reports.analytical.TabType;
import com.greenconnect.greenconnect_api.dto.reports.analytical.TimePreset;
import com.greenconnect.greenconnect_api.repositories.AnalyticalReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service cho Analytical Report API
 * - Xử lý logic tính toán báo cáo
 * - Caching 30 phút cho mỗi tab
 * - Rate limiting để tránh spam
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticalReportService {
    
    private final AnalyticalReportRepository analyticalReportRepository;
    
    // ==================== HELPER METHODS FOR SAFE TYPE CONVERSION ====================
    
    /**
     * Chuyển đổi an toàn Object sang Double
     * ✅ Đơn giản hơn BigDecimal, tương thích tốt với JDBC và Frontend Charts
     */
    private Double safeToDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Cannot convert {} to Double, returning 0.0", obj);
            return 0.0;
        }
    }
    
    /**
     * Chuyển đổi an toàn Object sang Integer
     * Xử lý các trường hợp: Integer, Long, BigInteger, null
     */
    private Integer safeToInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Cannot convert {} to Integer, returning 0", obj);
            return 0;
        }
    }
    
    /**
     * Chuyển đổi an toàn Object sang UUID
     * Xử lý các trường hợp: String, byte[] (MySQL binary), null
     */
    private UUID safeToUUID(Object obj) {
        if (obj == null) {
            log.warn("⚠️ UUID object is null, returning random UUID");
            return UUID.randomUUID();
        }
        
        try {
            if (obj instanceof String) {
                String str = ((String) obj).trim();
                if (str.isEmpty()) {
                    log.warn("⚠️ UUID string is empty, returning random UUID");
                    return UUID.randomUUID();
                }
                return UUID.fromString(str);
            }
            
            if (obj instanceof byte[]) {
                // MySQL có thể trả về UUID dưới dạng byte[]
                String str = new String((byte[]) obj).trim();
                if (str.isEmpty()) {
                    log.warn("⚠️ UUID byte[] is empty, returning random UUID");
                    return UUID.randomUUID();
                }
                return UUID.fromString(str);
            }
            
            // Fallback: convert toString
            String str = obj.toString().trim();
            if (str.isEmpty()) {
                log.warn("⚠️ UUID toString is empty, returning random UUID");
                return UUID.randomUUID();
            }
            return UUID.fromString(str);
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID string: {}, returning random UUID. Error: {}", obj, e.getMessage());
            return UUID.randomUUID();
        }
    }
    
    /**
     * Chuyển đổi an toàn Object sang String
     */
    private String safeToString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof byte[]) return new String((byte[]) obj);
        return obj.toString();
    }
    
    // ==================== CACHE MANAGEMENT ====================
    // Cache structure: Map<cacheKey, CacheEntry>
    // cacheKey = "tab_startDate_endDate"
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    private static final long CACHE_DURATION_MINUTES = 30;
    
    /**
     * Cache entry model
     */
    private static class CacheEntry {
        Object data;
        LocalDateTime cachedAt;
        LocalDateTime expiresAt;
        
        CacheEntry(Object data) {
            this.data = data;
            this.cachedAt = LocalDateTime.now();
            this.expiresAt = this.cachedAt.plusMinutes(CACHE_DURATION_MINUTES);
        }
        
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
        
        long getRemainingSeconds() {
            return Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        }
    }
    
    // ==================== MAIN ENTRY POINT ====================
    
    /**
     * Lấy báo cáo phân tích theo tab
     */
    public AnalyticalReportResponse getAnalyticalReport(
            String presetStr,
            LocalDate customStartDate,
            LocalDate customEndDate,
            TabType tab
    ) {
        log.info("📊 [AnalyticalReport] Request for tab: {}, preset: {}", tab, presetStr);
        
        // Parse time preset
        TimePreset preset = TimePreset.fromValue(presetStr);
        
        // Calculate date range
        LocalDateTime startDate;
        LocalDateTime endDate;
        
        if (preset == TimePreset.CUSTOM && customStartDate != null && customEndDate != null) {
            startDate = customStartDate.atStartOfDay();
            endDate = customEndDate.atTime(23, 59, 59);
        } else {
            LocalDateTime[] range = calculateDateRange(preset);
            startDate = range[0];
            endDate = range[1];
        }
        
        log.info("📅 [AnalyticalReport] Date range: {} to {}", startDate, endDate);
        
        // Generate cache key
        String cacheKey = generateCacheKey(tab, startDate, endDate);
        
        // Check cache
        CacheEntry cacheEntry = cache.get(cacheKey);
        if (cacheEntry != null && !cacheEntry.isExpired()) {
            log.info("✅ [AnalyticalReport] Cache HIT for key: {}", cacheKey);
            return buildResponseFromCache(tab, cacheEntry);
        }
        
        // Cache MISS or expired - load fresh data
        log.info("🔄 [AnalyticalReport] Cache MISS for key: {}, loading fresh data...", cacheKey);
        
        Object freshData = null;
        switch (tab) {
            case OVERVIEW -> freshData = loadOverviewData(startDate, endDate);
            case PRODUCT -> freshData = loadProductData(startDate, endDate);
            case CUSTOMER -> freshData = loadCustomerData(startDate, endDate);
        }
        
        // Save to cache
        CacheEntry newEntry = new CacheEntry(freshData);
        cache.put(cacheKey, newEntry);
        
        // Build response
        return buildResponseFromCache(tab, newEntry);
    }
    
    // ==================== DATE RANGE CALCULATION ====================
    
    private LocalDateTime[] calculateDateRange(TimePreset preset) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;
        
        switch (preset) {
            case SEVEN_DAYS -> start = now.minusDays(7);
            case THIRTY_DAYS -> start = now.minusDays(30);
            case NINETY_DAYS -> start = now.minusDays(90);
            // case THIS_MONTH -> start = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
            // case LAST_MONTH -> {
            //     start = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
            //     end = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59, 59);
            // }
            case THIS_YEAR -> start = now.with(TemporalAdjusters.firstDayOfYear()).toLocalDate().atStartOfDay();
            default -> start = now.minusDays(30); // Default 30 days
        }
        
        return new LocalDateTime[]{start, end};
    }
    
    // ==================== CACHE KEY GENERATION ====================
    
    private String generateCacheKey(TabType tab, LocalDateTime startDate, LocalDateTime endDate) {
        return String.format("%s_%s_%s", 
            tab.name(), 
            startDate.toLocalDate().toString(), 
            endDate.toLocalDate().toString()
        );
    }
    
    // ==================== LOAD DATA BY TAB ====================
    
    /**
     * Load Overview tab data
     */
    private OverviewResponse loadOverviewData(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("📊 [Overview] Loading data from {} to {}", startDate, endDate);
        
        // Get total revenue & orders
        Map<String, Object> totals = analyticalReportRepository.getTotalRevenueAndOrders(startDate, endDate);
        
        // ✅ Sử dụng Double - đơn giản hơn BigDecimal
        Double totalRevenue = safeToDouble(totals.get("totalRevenue"));
        Integer totalOrders = safeToInteger(totals.get("totalOrders"));
        
        // Calculate AOV - không cần RoundingMode
        Double aov = totalOrders > 0 ? totalRevenue / totalOrders : 0.0;
        
        // Calculate days in range
        int daysInRange = (int) Duration.between(startDate, endDate).toDays() + 1;
        
        // Calculate avg daily revenue
        Double avgDailyRevenue = daysInRange > 0 ? totalRevenue / daysInRange : 0.0;
        
        // Get monthly data for chart
        List<OverviewResponse.MonthlyRevenueData> monthlyData = getMonthlyRevenueChartData(startDate, endDate);
        
        // Get daily data for table
        List<OverviewResponse.DailyOperationData> dailyData = getDailyOperationTableData(startDate, endDate, totalRevenue);
        
        return OverviewResponse.builder()
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders)
            .aov(aov)
            .avgDailyRevenue(avgDailyRevenue)
            .daysInRange(daysInRange)
            .monthlyRevenueData(monthlyData)
            .dailyOperationData(dailyData)
            .build();
    }
    
    /**
     * Load Product tab data
     */
    private ProductResponse loadProductData(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("🛍️ [Product] Loading data from {} to {}", startDate, endDate);
        
        // Get top 3 selling products
        List<ProductResponse.TopProductData> topProducts = analyticalReportRepository
            .getTopSellingProducts(startDate, endDate, 3)
            .stream()
            .map(row -> {
                // ✅ Sử dụng helper methods
                UUID productId = safeToUUID(row.get("productId"));
                String productName = safeToString(row.get("productName"));
                String productImage = safeToString(row.get("productImage"));
                String categoryName = safeToString(row.get("categoryName"));
                Double revenue = safeToDouble(row.get("revenue"));
                Integer quantitySold = safeToInteger(row.get("quantitySold"));
                Double averageRating = safeToDouble(row.get("averageRating"));
                
                return ProductResponse.TopProductData.builder()
                    .productId(productId)
                    .productName(productName)
                    .productImage(productImage)
                    .categoryName(categoryName)
                    .revenue(revenue)
                    .quantitySold(quantitySold)
                    .averageRating(averageRating)
                    .build();
            })
            .collect(Collectors.toList());
        
        // Get top 3 returned products
        List<ProductResponse.ReturnedProductData> returnedProducts = analyticalReportRepository
            .getTopReturnedProducts(startDate, endDate, 3)
            .stream()
            .map(row -> {
                // ✅ Sử dụng helper methods
                UUID productId = safeToUUID(row.get("productId"));
                String productName = safeToString(row.get("productName"));
                String productImage = safeToString(row.get("productImage"));
                Double returnRate = safeToDouble(row.get("returnRate"));
                Integer returnCount = safeToInteger(row.get("returnCount"));
                String topReturnReason = safeToString(row.get("topReturnReason"));
                Integer totalOrders = safeToInteger(row.get("totalOrders"));
                
                return ProductResponse.ReturnedProductData.builder()
                    .productId(productId)
                    .productName(productName)
                    .productImage(productImage)
                    .returnRate(returnRate)
                    .returnCount(returnCount)
                    .topReturnReason(topReturnReason)
                    .totalOrders(totalOrders)
                    .build();
            })
            .collect(Collectors.toList());
        
        return ProductResponse.builder()
            .topProducts(topProducts)
            .returnedProducts(returnedProducts)
            .build();
    }
    
    /**
     * Load Customer tab data
     */
    private CustomerResponse loadCustomerData(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("👥 [Customer] Loading data from {} to {}", startDate, endDate);
        
        // Get total customers có đơn hàng trong khoảng thời gian
        Long totalCustomers = analyticalReportRepository.getTotalCustomersInRange(startDate, endDate);
        
        // Get new customers trong khoảng thời gian
        Long newCustomers = analyticalReportRepository.getNewCustomersInRange(startDate, endDate);
        
        // Get tổng số customer trong toàn hệ thống
        Long totalCustomersInSystem = analyticalReportRepository.getTotalCustomersInSystem();
        
        // Tính tỷ lệ khách hàng mới = (Số khách mới trong thời gian lọc) / (Tổng số customer trong hệ thống) * 100
        // Công thức này đảm bảo % luôn <= 100%
        Double newCustomerRate = totalCustomersInSystem > 0 
            ? (newCustomers.doubleValue() / totalCustomersInSystem.doubleValue()) * 100.0
            : 0.0;
        
        // Get customers by province
        List<CustomerResponse.CustomerByRegionData> customersByRegion = analyticalReportRepository
            .getCustomersByProvince(startDate, endDate)
            .stream()
            .map(row -> {
                // ✅ Sử dụng helper methods với Double
                String province = safeToString(row.get("province"));
                Integer count = safeToInteger(row.get("customerCount"));
                Double percentage = totalCustomers > 0 
                    ? (count.doubleValue() / totalCustomers.doubleValue()) * 100.0
                    : 0.0;
                
                return CustomerResponse.CustomerByRegionData.builder()
                    .province(province)
                    .count(count)
                    .percentage(percentage)
                    .build();
            })
            .collect(Collectors.toList());
        
        // Get top 4 VIP customers
        List<CustomerResponse.VipCustomerData> vipCustomers = analyticalReportRepository
            .getTopVipCustomers(startDate, endDate, 4)
            .stream()
            .map(row -> {
                // ✅ Sử dụng helper methods với Double
                UUID customerId = safeToUUID(row.get("customerId"));
                String name = safeToString(row.get("name"));
                String email = safeToString(row.get("email"));
                Double totalSpent = safeToDouble(row.get("totalSpent"));
                Integer orderCount = safeToInteger(row.get("orderCount"));
                Double averageRating = safeToDouble(row.get("averageRating"));
                
                return CustomerResponse.VipCustomerData.builder()
                    .customerId(customerId)
                    .name(name)
                    .email(email)
                    .totalSpent(totalSpent)
                    .orderCount(orderCount)
                    .averageRating(averageRating)
                    .build();
            })
            .collect(Collectors.toList());
        
        return CustomerResponse.builder()
            .totalCustomers(totalCustomers.intValue())
            .newCustomerCount(newCustomers.intValue())
            .newCustomerRate(newCustomerRate)
            .customersByRegion(customersByRegion)
            .vipCustomers(vipCustomers)
            .build();
    }

    /**
     * Lấy danh sách VIP customers với phân trang
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @param page Số trang (0-indexed)
     * @param size Kích thước trang (mặc định 10)
     * @return Page chứa danh sách VIP customers
     */
    public org.springframework.data.domain.Page<CustomerResponse.VipCustomerData> getVipCustomersPaginated(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        log.info("🔍 Lấy VIP customers - page: {}, size: {}, từ {} đến {}", page, size, start, end);
        
        // Lấy danh sách customers cho trang hiện tại
        List<CustomerResponse.VipCustomerData> customers = analyticalReportRepository
            .getVipCustomersPaginated(start, end, page, size)
            .stream()
            .map(row -> {
                UUID customerId = safeToUUID(row.get("customerId"));
                String name = safeToString(row.get("name"));
                String email = safeToString(row.get("email"));
                Double totalSpent = safeToDouble(row.get("totalSpent"));
                Integer orderCount = safeToInteger(row.get("orderCount"));
                Double averageRating = safeToDouble(row.get("averageRating"));
                
                return CustomerResponse.VipCustomerData.builder()
                    .customerId(customerId)
                    .name(name)
                    .email(email)
                    .totalSpent(totalSpent)
                    .orderCount(orderCount)
                    .averageRating(averageRating)
                    .build();
            })
            .collect(Collectors.toList());
        
        // Đếm tổng số customers
        long totalElements = analyticalReportRepository.countVipCustomers(start, end);
        
        log.info("✅ Tìm thấy {} VIP customers (tổng: {})", customers.size(), totalElements);
        
        // Tạo Page object
        return new org.springframework.data.domain.PageImpl<>(
            customers,
            org.springframework.data.domain.PageRequest.of(page, size),
            totalElements
        );
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Get monthly revenue chart data
     */
    private List<OverviewResponse.MonthlyRevenueData> getMonthlyRevenueChartData(
            LocalDateTime startDate, 
            LocalDateTime endDate
    ) {
        List<Map<String, Object>> rawData = analyticalReportRepository.getMonthlyRevenueData(startDate, endDate);
        
        return rawData.stream()
            .map(row -> {
                // ✅ Sử dụng Double - đơn giản
                int month = safeToInteger(row.get("month"));
                Double revenue = safeToDouble(row.get("revenue"));
                Integer orderCount = safeToInteger(row.get("orderCount"));
                
                Double aov = orderCount > 0 ? revenue / orderCount : 0.0;
                
                return OverviewResponse.MonthlyRevenueData.builder()
                    .month("T" + month)
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .aov(aov)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get daily operation table data
     */
    private List<OverviewResponse.DailyOperationData> getDailyOperationTableData(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double totalRevenue
    ) {
        List<Map<String, Object>> rawData = analyticalReportRepository.getDailyRevenueData(startDate, endDate);
        
        return rawData.stream()
            .map(row -> {
                // ✅ Sử dụng helper methods
                LocalDate date = null;
                Object dateObj = row.get("orderDate");
                if (dateObj instanceof java.sql.Date) {
                    date = ((java.sql.Date) dateObj).toLocalDate();
                } else if (dateObj instanceof LocalDate) {
                    date = (LocalDate) dateObj;
                }
                
                Double revenue = safeToDouble(row.get("revenue"));
                Integer orderCount = safeToInteger(row.get("orderCount"));
                
                Double aov = orderCount > 0 ? revenue / orderCount : 0.0;
                
                Double contributionRate = totalRevenue > 0.0 
                    ? (revenue / totalRevenue) * 100.0
                    : 0.0;
                
                return OverviewResponse.DailyOperationData.builder()
                    .date(date)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .aov(aov)
                    .contributionRate(contributionRate)
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    // ==================== RESPONSE BUILDER ====================
    
    private AnalyticalReportResponse buildResponseFromCache(TabType tab, CacheEntry cacheEntry) {
        AnalyticalReportResponse.AnalyticalReportResponseBuilder responseBuilder = AnalyticalReportResponse.builder()
            .success(true)
            .message("Báo cáo tải thành công")
            .tab(tab)
            .timestamp(LocalDateTime.now());
        
        // Set data based on tab
        switch (tab) {
            case OVERVIEW -> responseBuilder.overview((OverviewResponse) cacheEntry.data);
            case PRODUCT -> responseBuilder.product((ProductResponse) cacheEntry.data);
            case CUSTOMER -> responseBuilder.customer((CustomerResponse) cacheEntry.data);
        }
        
        // Add cache info
        long remainingSeconds = cacheEntry.getRemainingSeconds();
        String rateLimitMessage = remainingSeconds > 0
            ? String.format("Dữ liệu từ cache. Có thể tải lại sau %d phút %d giây", 
                remainingSeconds / 60, remainingSeconds % 60)
            : "Dữ liệu mới tải";
        
        responseBuilder.cacheInfo(AnalyticalReportResponse.CacheInfo.builder()
            .fromCache(!cacheEntry.isExpired())
            .cachedAt(cacheEntry.cachedAt)
            .expiresAt(cacheEntry.expiresAt)
            .remainingSeconds(remainingSeconds)
            .rateLimitMessage(rateLimitMessage)
            .build());
        
        return responseBuilder.build();
    }
    
    /**
     * Clear cache (for admin maintenance)
     */
    public void clearCache() {
        cache.clear();
        log.info("🗑️ [AnalyticalReport] Cache cleared");
    }
    
    /**
     * Clear specific tab cache
     */
    public void clearCacheForTab(TabType tab) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(tab.name()));
        log.info("🗑️ [AnalyticalReport] Cache cleared for tab: {}", tab);
    }
}

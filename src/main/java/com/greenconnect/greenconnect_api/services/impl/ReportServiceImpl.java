package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.response.report.BusinessTargetsResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomerRetentionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.CustomersByRegionResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.MonthlyRevenueComparisonResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.OrdersHeatmapResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.VipCustomersResponse;
import com.greenconnect.greenconnect_api.dtos.response.report.WeeklyTrendsResponse;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.services.ReportService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    @PersistenceContext
    private EntityManager entityManager;
    
    private static final List<String> DAY_NAMES = Arrays.asList("T2", "T3", "T4", "T5", "T6", "T7", "CN");
    private static final List<String> MONTH_NAMES = Arrays.asList(
        "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"
    );

    @Override
    public MonthlyRevenueComparisonResponse getMonthlyRevenueComparison(int year, Integer compareYear) {
        log.info("📊 [Report] So sánh doanh thu năm {} vs {}", year, compareYear);
        
        int compYear = compareYear != null ? compareYear : year - 1;
        
        // Query doanh thu theo tháng cho cả 2 năm
        String jpql = """
            SELECT MONTH(o.orderDate) as month,
                   YEAR(o.orderDate) as year,
                   COALESCE(SUM(o.totalPayment), 0) as revenue
            FROM Order o
            WHERE o.orderStatus = :status
              AND YEAR(o.orderDate) IN (:year1, :year2)
            GROUP BY YEAR(o.orderDate), MONTH(o.orderDate)
            ORDER BY year, month
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("status", OrderStatus.DA_GIAO)
                .setParameter("year1", year)
                .setParameter("year2", compYear)
                .getResultList();
        
        // Tạo map để tra cứu
        Map<String, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            int yr = ((Number) row[1]).intValue();
            BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            revenueMap.put(yr + "-" + month, revenue);
        }
        
        // Tạo kết quả cho 12 tháng
        List<MonthlyRevenueComparisonResponse.MonthlyRevenue> monthlyData = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal thisYearRevenue = revenueMap.getOrDefault(year + "-" + m, BigDecimal.ZERO);
            BigDecimal lastYearRevenue = revenueMap.getOrDefault(compYear + "-" + m, BigDecimal.ZERO);
            
            Double growthRate = 0.0;
            if (lastYearRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = thisYearRevenue.subtract(lastYearRevenue)
                        .divide(lastYearRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                growthRate = Math.round(growthRate * 10.0) / 10.0;
            } else if (thisYearRevenue.compareTo(BigDecimal.ZERO) > 0) {
                growthRate = 100.0;
            }
            
            monthlyData.add(MonthlyRevenueComparisonResponse.MonthlyRevenue.builder()
                    .month(MONTH_NAMES.get(m - 1))
                    .monthNumber(m)
                    .thisYear(thisYearRevenue)
                    .lastYear(lastYearRevenue)
                    .growthRate(growthRate)
                    .build());
        }
        
        return MonthlyRevenueComparisonResponse.builder()
                .year(year)
                .compareYear(compYear)
                .data(monthlyData)
                .build();
    }

    @Override
    public CustomersByRegionResponse getCustomersByRegion(LocalDate startDate, LocalDate endDate) {
        log.info("📊 [Report] Phân bổ khách hàng theo khu vực");
        
        // Lấy tỉnh/thành từ địa chỉ mặc định của khách hàng
        String jpql = """
            SELECT COALESCE(a.provinceName63, a.provinceName34, 'Khác') as region,
                   COUNT(DISTINCT u.id) as count
            FROM User u
            JOIN u.userRoles ur
            LEFT JOIN Address a ON a.user = u AND a.isDefault = true
            WHERE ur.role = :role AND ur.active = true
            GROUP BY COALESCE(a.provinceName63, a.provinceName34, 'Khác')
            ORDER BY count DESC
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("role", Role.CUSTOMER)
                .getResultList();
        
        // Tính tổng
        Long totalCustomers = results.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
        
        // Lấy top 5 + gom còn lại vào "Khác"
        List<CustomersByRegionResponse.RegionData> regions = new ArrayList<>();
        Long otherCount = 0L;
        
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            String region = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            
            if (i < 5 && !"Khác".equals(region)) {
                Double percentage = totalCustomers > 0 
                        ? Math.round(count * 1000.0 / totalCustomers) / 10.0 
                        : 0.0;
                
                regions.add(CustomersByRegionResponse.RegionData.builder()
                        .region(region)
                        .count(count)
                        .percentage(percentage)
                        .build());
            } else {
                otherCount += count;
            }
        }
        
        // Thêm "Khác" nếu có
        if (otherCount > 0) {
            Double percentage = totalCustomers > 0 
                    ? Math.round(otherCount * 1000.0 / totalCustomers) / 10.0 
                    : 0.0;
            regions.add(CustomersByRegionResponse.RegionData.builder()
                    .region("Khác")
                    .count(otherCount)
                    .percentage(percentage)
                    .build());
        }
        
        return CustomersByRegionResponse.builder()
                .totalCustomers(totalCustomers)
                .regions(regions)
                .build();
    }

    @Override
    public CustomerRetentionResponse getCustomerRetention(LocalDate startDate, LocalDate endDate) {
        log.info("📊 [Report] Tỷ lệ khách hàng quay lại");
        
        LocalDateTime start = startDate != null 
                ? startDate.atStartOfDay() 
                : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null 
                ? endDate.atTime(LocalTime.MAX) 
                : LocalDate.now().atTime(LocalTime.MAX);
        
        // Đếm khách hàng có >= 2 đơn hàng (quay lại) trong khoảng thời gian
        String returningJpql = """
            SELECT COUNT(DISTINCT o.user.id)
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
              AND o.orderStatus = :status
            GROUP BY o.user.id
            HAVING COUNT(o) >= 2
            """;
        
        List<Long> returningResults = entityManager.createQuery(returningJpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("status", OrderStatus.DA_GIAO)
                .getResultList();
        Long returningCustomers = (long) returningResults.size();
        
        // Đếm khách hàng chỉ có 1 đơn hàng (mới)
        String newCustomerJpql = """
            SELECT COUNT(DISTINCT o.user.id)
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
              AND o.orderStatus = :status
            GROUP BY o.user.id
            HAVING COUNT(o) = 1
            """;
        
        List<Long> newResults = entityManager.createQuery(newCustomerJpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("status", OrderStatus.DA_GIAO)
                .getResultList();
        Long newCustomers = (long) newResults.size();
        
        Long totalCustomers = returningCustomers + newCustomers;
        
        Double retentionRate = totalCustomers > 0 
                ? Math.round(returningCustomers * 1000.0 / totalCustomers) / 10.0 
                : 0.0;
        Double newCustomerRate = totalCustomers > 0 
                ? Math.round(newCustomers * 1000.0 / totalCustomers) / 10.0 
                : 0.0;
        
        return CustomerRetentionResponse.builder()
                .retentionRate(retentionRate)
                .newCustomerRate(newCustomerRate)
                .totalCustomers(totalCustomers)
                .returningCustomers(returningCustomers)
                .newCustomers(newCustomers)
                .build();
    }

    @Override
    public BusinessTargetsResponse getBusinessTargets(int month, int year) {
        log.info("📊 [Report] Mục tiêu kinh doanh tháng {}/{}", month, year);
        
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime start = startOfMonth.atStartOfDay();
        LocalDateTime end = endOfMonth.atTime(LocalTime.MAX);
        
        // === DOANH THU ===
        String revenueJpql = """
            SELECT COALESCE(SUM(o.totalPayment), 0)
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
              AND o.orderStatus = :status
            """;
        BigDecimal currentRevenue = entityManager.createQuery(revenueJpql, BigDecimal.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("status", OrderStatus.DA_GIAO)
                .getSingleResult();
        
        // === ĐƠN HÀNG ===
        String ordersJpql = """
            SELECT COUNT(o)
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
            """;
        Long currentOrders = entityManager.createQuery(ordersJpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        
        // === KHÁCH HÀNG MỚI ===
        String newCustomersJpql = """
            SELECT COUNT(DISTINCT u)
            FROM User u
            JOIN u.userRoles ur
            WHERE u.createdAt >= :start AND u.createdAt <= :end
              AND ur.role = :role AND ur.active = true
            """;
        Long currentNewCustomers = entityManager.createQuery(newCustomersJpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("role", Role.CUSTOMER)
                .getSingleResult();
        
        // === ĐÁNH GIÁ 5 SAO ===
        String fiveStarJpql = """
            SELECT COUNT(pr)
            FROM ProductReview pr
            WHERE pr.reviewTime >= :start AND pr.reviewTime <= :end
              AND pr.rating = 5 AND pr.isApproved = true
            """;
        Long currentFiveStarReviews = entityManager.createQuery(fiveStarJpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        
        // Mục tiêu (có thể lấy từ database hoặc config, tạm thời hardcode)
        BigDecimal revenueTarget = new BigDecimal("1000000000"); // 1 tỷ
        Long ordersTarget = 1500L;
        Long newCustomersTarget = 500L;
        Long fiveStarTarget = 100L;
        
        List<BusinessTargetsResponse.TargetData> targets = Arrays.asList(
                BusinessTargetsResponse.TargetData.builder()
                        .id("revenue")
                        .name("Doanh thu")
                        .current(currentRevenue)
                        .target(revenueTarget)
                        .unit("VNĐ")
                        .percentage(calculatePercentage(currentRevenue, revenueTarget))
                        .color("#4CAF50")
                        .build(),
                BusinessTargetsResponse.TargetData.builder()
                        .id("orders")
                        .name("Đơn hàng")
                        .current(BigDecimal.valueOf(currentOrders))
                        .target(BigDecimal.valueOf(ordersTarget))
                        .unit("đơn")
                        .percentage(calculatePercentage(currentOrders, ordersTarget))
                        .color("#2196F3")
                        .build(),
                BusinessTargetsResponse.TargetData.builder()
                        .id("new_customers")
                        .name("Khách mới")
                        .current(BigDecimal.valueOf(currentNewCustomers))
                        .target(BigDecimal.valueOf(newCustomersTarget))
                        .unit("người")
                        .percentage(calculatePercentage(currentNewCustomers, newCustomersTarget))
                        .color("#03A9F4")
                        .build(),
                BusinessTargetsResponse.TargetData.builder()
                        .id("five_star_reviews")
                        .name("Đánh giá 5⭐")
                        .current(BigDecimal.valueOf(currentFiveStarReviews))
                        .target(BigDecimal.valueOf(fiveStarTarget))
                        .unit("đánh giá")
                        .percentage(calculatePercentage(currentFiveStarReviews, fiveStarTarget))
                        .color("#FF9800")
                        .build()
        );
        
        return BusinessTargetsResponse.builder()
                .period(month + "/" + year)
                .targets(targets)
                .build();
    }

    @Override
    public OrdersHeatmapResponse getOrdersHeatmap(LocalDate startDate, LocalDate endDate) {
        log.info("📊 [Report] Heatmap đơn hàng theo giờ/ngày");
        
        LocalDateTime start = startDate != null 
                ? startDate.atStartOfDay() 
                : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null 
                ? endDate.atTime(LocalTime.MAX) 
                : LocalDate.now().atTime(LocalTime.MAX);
        
        // Query đếm đơn hàng theo ngày trong tuần và giờ
        String jpql = """
            SELECT DAYOFWEEK(o.orderDate) as dayOfWeek,
                   HOUR(o.orderDate) as hour,
                   COUNT(o) as orderCount
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
            GROUP BY DAYOFWEEK(o.orderDate), HOUR(o.orderDate)
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        
        // Tạo map [dayOfWeek][hour] -> count
        // MySQL DAYOFWEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
        // Chuyển đổi: 2->0(T2), 3->1(T3), ..., 7->5(T7), 1->6(CN)
        int[][] heatmapMatrix = new int[7][24];
        int maxOrders = 0;
        
        for (Object[] row : results) {
            int mysqlDay = ((Number) row[0]).intValue();
            int hour = ((Number) row[1]).intValue();
            int count = ((Number) row[2]).intValue();
            
            // Chuyển đổi: MySQL Sunday=1 -> dayIndex=6, Monday=2 -> dayIndex=0
            int dayIndex = (mysqlDay == 1) ? 6 : mysqlDay - 2;
            
            heatmapMatrix[dayIndex][hour] = count;
            if (count > maxOrders) {
                maxOrders = count;
            }
        }
        
        // Tạo response
        List<OrdersHeatmapResponse.HeatmapDay> heatmapData = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<Integer> hourlyData = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                hourlyData.add(heatmapMatrix[d][h]);
            }
            
            heatmapData.add(OrdersHeatmapResponse.HeatmapDay.builder()
                    .dayIndex(d)
                    .dayName(DAY_NAMES.get(d))
                    .hourlyData(hourlyData)
                    .build());
        }
        
        List<Integer> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(h);
        }
        
        return OrdersHeatmapResponse.builder()
                .days(DAY_NAMES)
                .hours(hours)
                .maxOrders(maxOrders)
                .heatmapData(heatmapData)
                .build();
    }

    @Override
    public VipCustomersResponse getVipCustomers(int limit, LocalDate startDate, LocalDate endDate, String sortBy) {
        log.info("📊 [Report] Top {} khách hàng VIP", limit);
        
        LocalDateTime start = startDate != null 
                ? startDate.atStartOfDay() 
                : LocalDate.now().minusDays(365).atStartOfDay(); // Mặc định 1 năm
        LocalDateTime end = endDate != null 
                ? endDate.atTime(LocalTime.MAX) 
                : LocalDate.now().atTime(LocalTime.MAX);
        
        String orderByClause = switch (sortBy != null ? sortBy.toLowerCase() : "totalspent") {
            case "ordercount" -> "orderCount DESC";
            case "rating" -> "avgRating DESC";
            default -> "totalSpent DESC";
        };
        
        String jpql = """
            SELECT u.id as userId,
                   u.fullName as name,
                   u.email as email,
                   u.phoneNumber as phone,
                   u.avatarUrl as avatar,
                   COALESCE(SUM(o.totalPayment), 0) as totalSpent,
                   COUNT(o) as orderCount,
                   u.createdAt as memberSince,
                   MAX(o.orderDate) as lastOrderDate
            FROM Order o
            JOIN o.user u
            WHERE o.orderDate >= :start AND o.orderDate <= :end
              AND o.orderStatus = :status
            GROUP BY u.id, u.fullName, u.email, u.phoneNumber, u.avatarUrl, u.createdAt
            ORDER BY\s""" + orderByClause;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("status", OrderStatus.DA_GIAO)
                .setMaxResults(limit)
                .getResultList();
        
        // Lấy average rating cho mỗi khách hàng từ reviews họ đã để lại
        List<VipCustomersResponse.VipCustomer> customers = results.stream()
                .map(row -> {
                    UUID userId = (UUID) row[0];
                    
                    // Lấy avg rating của customer
                    String ratingJpql = """
                        SELECT COALESCE(AVG(pr.rating), 0.0)
                        FROM ProductReview pr
                        WHERE pr.user.id = :userId AND pr.isApproved = true
                        """;
                    Double avgRating = entityManager.createQuery(ratingJpql, Double.class)
                            .setParameter("userId", userId)
                            .getSingleResult();
                    
                    return VipCustomersResponse.VipCustomer.builder()
                            .id(userId)
                            .name((String) row[1])
                            .email((String) row[2])
                            .phone((String) row[3])
                            .avatar((String) row[4])
                            .totalSpent((BigDecimal) row[5])
                            .orderCount((Long) row[6])
                            .memberSince((LocalDateTime) row[7])
                            .lastOrderDate((LocalDateTime) row[8])
                            .averageRating(Math.round(avgRating * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Đếm tổng số VIP customers (tổng chi tiêu >= 15 triệu)
        String countJpql = """
            SELECT COUNT(DISTINCT u.id)
            FROM Order o
            JOIN o.user u
            WHERE o.orderStatus = :status
            GROUP BY u.id
            HAVING SUM(o.totalPayment) >= 15000000
            """;
        List<Long> countResults = entityManager.createQuery(countJpql, Long.class)
                .setParameter("status", OrderStatus.DA_GIAO)
                .getResultList();
        Long totalVipCustomers = (long) countResults.size();
        
        return VipCustomersResponse.builder()
                .totalVipCustomers(totalVipCustomers)
                .customers(customers)
                .build();
    }

    @Override
    public WeeklyTrendsResponse getWeeklyTrends(LocalDate endDate) {
        log.info("📊 [Report] Xu hướng 7 ngày gần nhất");
        
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = end.minusDays(6);
        
        List<Double> orderTrend = new ArrayList<>();
        List<Double> revenueTrend = new ArrayList<>();
        List<Double> newCustomerTrend = new ArrayList<>();
        List<Double> ratingTrend = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            
            // Orders count
            String ordersJpql = """
                SELECT COUNT(o) FROM Order o 
                WHERE o.orderDate >= :start AND o.orderDate <= :end
                """;
            Long orders = entityManager.createQuery(ordersJpql, Long.class)
                    .setParameter("start", dayStart)
                    .setParameter("end", dayEnd)
                    .getSingleResult();
            orderTrend.add(orders.doubleValue());
            
            // Revenue (in millions)
            String revenueJpql = """
                SELECT COALESCE(SUM(o.totalPayment), 0) FROM Order o 
                WHERE o.orderDate >= :start AND o.orderDate <= :end
                  AND o.orderStatus = :status
                """;
            BigDecimal revenue = entityManager.createQuery(revenueJpql, BigDecimal.class)
                    .setParameter("start", dayStart)
                    .setParameter("end", dayEnd)
                    .setParameter("status", OrderStatus.DA_GIAO)
                    .getSingleResult();
            revenueTrend.add(revenue.divide(BigDecimal.valueOf(1000000), 1, RoundingMode.HALF_UP).doubleValue());
            
            // New customers
            String customersJpql = """
                SELECT COUNT(DISTINCT u) FROM User u
                JOIN u.userRoles ur
                WHERE u.createdAt >= :start AND u.createdAt <= :end
                  AND ur.role = :role AND ur.active = true
                """;
            Long newCustomers = entityManager.createQuery(customersJpql, Long.class)
                    .setParameter("start", dayStart)
                    .setParameter("end", dayEnd)
                    .setParameter("role", Role.CUSTOMER)
                    .getSingleResult();
            newCustomerTrend.add(newCustomers.doubleValue());
            
            // Average rating
            String ratingJpql = """
                SELECT COALESCE(AVG(pr.rating), 0.0) FROM ProductReview pr
                WHERE pr.reviewTime >= :start AND pr.reviewTime <= :end
                  AND pr.isApproved = true
                """;
            Double avgRating = entityManager.createQuery(ratingJpql, Double.class)
                    .setParameter("start", dayStart)
                    .setParameter("end", dayEnd)
                    .getSingleResult();
            ratingTrend.add(Math.round(avgRating * 10.0) / 10.0);
            
            // Label (T2, T3, ...)
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            int dayIndex = dayOfWeek.getValue() - 1; // Monday=0
            labels.add(DAY_NAMES.get(dayIndex));
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String period = start.format(formatter) + " - " + end.format(formatter);
        
        return WeeklyTrendsResponse.builder()
                .period(period)
                .orderTrend(orderTrend)
                .revenueTrend(revenueTrend)
                .newCustomerTrend(newCustomerTrend)
                .ratingTrend(ratingTrend)
                .labels(labels)
                .build();
    }
    
    // ==================== HELPER METHODS ====================
    
    private Double calculatePercentage(BigDecimal current, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return current.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
    
    private Double calculatePercentage(Long current, Long target) {
        if (target == 0) {
            return 0.0;
        }
        return Math.round(current * 1000.0 / target) / 10.0;
    }
}

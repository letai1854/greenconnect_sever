package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.response.dashboard.ComparisonStatsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.OrderStatusChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.RevenueChartResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopCategoriesResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopProductsResponse;
import com.greenconnect.greenconnect_api.dtos.response.dashboard.TopSuppliersResponse;
import com.greenconnect.greenconnect_api.entities.Order;
import com.greenconnect.greenconnect_api.entities.OrderDetail;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.repositories.OrderRepository;
import com.greenconnect.greenconnect_api.repositories.ProductReviewRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.DashboardService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductReviewRepository productReviewRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ComparisonStatsResponse getComparisonStats() {
        log.info("📊 [Dashboard] Lấy thống kê so sánh tuần này vs tuần trước");
        
        // Tính toán khoảng thời gian tuần này và tuần trước
        LocalDate today = LocalDate.now();
        LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        
        LocalDateTime thisWeekStart = startOfThisWeek.atStartOfDay();
        LocalDateTime thisWeekEnd = today.atTime(LocalTime.MAX);
        LocalDateTime lastWeekStart = startOfLastWeek.atStartOfDay();
        LocalDateTime lastWeekEnd = startOfThisWeek.minusDays(1).atTime(LocalTime.MAX);
        
        // Chỉ tính đơn hàng đã giao thành công (DA_GIAO)
        List<OrderStatus> completedStatuses = List.of(OrderStatus.DA_GIAO);
        
        // === DOANH THU ===
        BigDecimal currentRevenue = calculateRevenue(thisWeekStart, thisWeekEnd, completedStatuses);
        BigDecimal previousRevenue = calculateRevenue(lastWeekStart, lastWeekEnd, completedStatuses);
        Double revenueTrend = calculateTrend(currentRevenue, previousRevenue);
        
        // === SỐ ĐƠN HÀNG ===
        Long currentOrders = countOrders(thisWeekStart, thisWeekEnd, null);
        Long previousOrders = countOrders(lastWeekStart, lastWeekEnd, null);
        Double ordersTrend = calculateTrend(currentOrders, previousOrders);
        
        // === KHÁCH HÀNG MỚI ===
        Long currentNewCustomers = countNewCustomers(thisWeekStart, thisWeekEnd);
        Long previousNewCustomers = countNewCustomers(lastWeekStart, lastWeekEnd);
        Double newCustomersTrend = calculateTrend(currentNewCustomers, previousNewCustomers);
        
        // === ĐÁNH GIÁ TRUNG BÌNH ===
        Double currentAverageRating = calculateAverageRating(thisWeekStart, thisWeekEnd);
        Double previousAverageRating = calculateAverageRating(lastWeekStart, lastWeekEnd);
        Double ratingTrend = calculateTrend(currentAverageRating, previousAverageRating);
        
        return ComparisonStatsResponse.builder()
                .currentRevenue(currentRevenue)
                .previousRevenue(previousRevenue)
                .revenueTrend(revenueTrend)
                .currentOrders(currentOrders)
                .previousOrders(previousOrders)
                .ordersTrend(ordersTrend)
                .currentNewCustomers(currentNewCustomers)
                .previousNewCustomers(previousNewCustomers)
                .newCustomersTrend(newCustomersTrend)
                .currentAverageRating(currentAverageRating)
                .previousAverageRating(previousAverageRating)
                .ratingTrend(ratingTrend)
                .build();
    }

    @Override
    public RevenueChartResponse getRevenueChart(int days) {
        log.info("📊 [Dashboard] Lấy biểu đồ doanh thu {} ngày", days);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        // Chỉ tính đơn đã giao thành công
        String jpql = """
            SELECT CAST(o.orderDate AS LocalDate) as orderDay, 
                   SUM(o.totalPayment) as revenue, 
                   COUNT(o) as orderCount
            FROM Order o 
            WHERE o.orderDate >= :startDate 
              AND o.orderDate <= :endDate
              AND o.orderStatus = :status
            GROUP BY CAST(o.orderDate AS LocalDate)
            ORDER BY orderDay ASC
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(LocalTime.MAX))
                .setParameter("status", OrderStatus.DA_GIAO)
                .getResultList();
        
        // Tạo map từ kết quả query
        Map<LocalDate, Object[]> dataMap = new HashMap<>();
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            dataMap.put(date, row);
        }
        
        // Tạo list đầy đủ các ngày (fill 0 cho ngày không có dữ liệu)
        List<RevenueChartResponse.DailyRevenue> dailyRevenues = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal maxRevenue = BigDecimal.ZERO;
        LocalDate maxRevenueDate = startDate;
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Object[] row = dataMap.get(date);
            BigDecimal revenue = row != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long orderCount = row != null ? (Long) row[2] : 0L;
            
            dailyRevenues.add(RevenueChartResponse.DailyRevenue.builder()
                    .date(date)
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .build());
            
            totalRevenue = totalRevenue.add(revenue);
            if (revenue.compareTo(maxRevenue) > 0) {
                maxRevenue = revenue;
                maxRevenueDate = date;
            }
        }
        
        BigDecimal averageRevenue = days > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;
        
        return RevenueChartResponse.builder()
                .dailyRevenues(dailyRevenues)
                .totalRevenue(totalRevenue)
                .averageRevenue(averageRevenue)
                .maxRevenue(maxRevenue)
                .maxRevenueDate(maxRevenueDate)
                .build();
    }

    @Override
    public TopProductsResponse getTopProducts(int limit) {
        log.info("📊 [Dashboard] Lấy Top {} sản phẩm bán chạy", limit);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        String jpql = """
            SELECT od.variant.product.id as productId,
                   od.productName as productName,
                   od.productImageUrl as productImage,
                   od.variant.product.category.name as categoryName,
                   SUM(od.quantity) as quantitySold,
                   SUM(od.sellingPricePerUnit * od.quantity) as revenue,
                   od.variant.product.averageRating as averageRating
            FROM OrderDetail od
            JOIN od.order o
            WHERE o.orderDate >= :startDate 
              AND o.orderDate <= :endDate
              AND o.orderStatus = :status
            GROUP BY od.variant.product.id, od.productName, od.productImageUrl, 
                     od.variant.product.category.name, od.variant.product.averageRating
            ORDER BY quantitySold DESC
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(LocalTime.MAX))
                .setParameter("status", OrderStatus.DA_GIAO)
                .setMaxResults(limit)
                .getResultList();
        
        List<TopProductsResponse.TopProduct> products = results.stream()
                .map(row -> TopProductsResponse.TopProduct.builder()
                        .productId((java.util.UUID) row[0])
                        .productName((String) row[1])
                        .productImage((String) row[2])
                        .categoryName((String) row[3])
                        .quantitySold((Long) row[4])
                        .revenue((BigDecimal) row[5])
                        .averageRating(row[6] != null ? ((BigDecimal) row[6]).doubleValue() : 0.0)
                        .build())
                .collect(Collectors.toList());
        
        return TopProductsResponse.builder()
                .products(products)
                .build();
    }

    @Override
    public OrderStatusChartResponse getOrderStatusChart() {
        log.info("📊 [Dashboard] Lấy phân bố trạng thái đơn hàng");
        
        String jpql = """
            SELECT o.orderStatus as status, COUNT(o) as count
            FROM Order o
            GROUP BY o.orderStatus
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .getResultList();
        
        Long totalOrders = results.stream()
                .mapToLong(row -> (Long) row[1])
                .sum();
        
        List<OrderStatusChartResponse.StatusCount> statusCounts = results.stream()
                .map(row -> {
                    OrderStatus status = (OrderStatus) row[0];
                    Long count = (Long) row[1];
                    Double percentage = totalOrders > 0 
                            ? (count.doubleValue() / totalOrders.doubleValue()) * 100 
                            : 0.0;
                    
                    return OrderStatusChartResponse.StatusCount.builder()
                            .status(status)
                            .statusLabel(getStatusLabel(status))
                            .count(count)
                            .percentage(Math.round(percentage * 10.0) / 10.0)
                            .color(getStatusColor(status))
                            .build();
                })
                .collect(Collectors.toList());
        
        return OrderStatusChartResponse.builder()
                .statusCounts(statusCounts)
                .totalOrders(totalOrders)
                .build();
    }

    @Override
    public TopCategoriesResponse getTopCategories(int limit) {
        log.info("📊 [Dashboard] Lấy Top {} danh mục", limit);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        // Query các danh mục có doanh thu trong 30 ngày
        String jpql = """
            SELECT c.id as categoryId,
                   c.name as categoryName,
                   c.imageUrl as categoryImage,
                   COUNT(DISTINCT p.id) as productCount,
                   SUM(od.quantity) as quantitySold,
                   SUM(od.sellingPricePerUnit * od.quantity) as revenue
            FROM OrderDetail od
            JOIN od.order o
            JOIN od.variant.product p
            JOIN p.category c
            WHERE o.orderDate >= :startDate 
              AND o.orderDate <= :endDate
              AND o.orderStatus = :status
            GROUP BY c.id, c.name, c.imageUrl
            ORDER BY revenue DESC
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(LocalTime.MAX))
                .setParameter("status", OrderStatus.DA_GIAO)
                .setMaxResults(limit)
                .getResultList();
        
        List<TopCategoriesResponse.TopCategory> categories = new ArrayList<>(results.stream()
                .map(row -> TopCategoriesResponse.TopCategory.builder()
                        .categoryId((java.util.UUID) row[0])
                        .categoryName((String) row[1])
                        .categoryImage((String) row[2])
                        .productCount((Long) row[3])
                        .quantitySold((Long) row[4])
                        .revenue((BigDecimal) row[5])
                        .build())
                .collect(Collectors.toList()));
        
        // Nếu chưa đủ limit, bổ sung thêm các danh mục khác (không có doanh thu)
        if (categories.size() < limit) {
            List<java.util.UUID> existingIds = categories.stream()
                    .map(TopCategoriesResponse.TopCategory::getCategoryId)
                    .collect(Collectors.toList());
            
            String fillJpql = existingIds.isEmpty() 
                ? "SELECT c.id, c.name, c.imageUrl FROM Category c ORDER BY c.name ASC"
                : "SELECT c.id, c.name, c.imageUrl FROM Category c WHERE c.id NOT IN :existingIds ORDER BY c.name ASC";
            
            var query = entityManager.createQuery(fillJpql, Object[].class);
            if (!existingIds.isEmpty()) {
                query.setParameter("existingIds", existingIds);
            }
            
            List<Object[]> additionalCategories = query
                    .setMaxResults(limit - categories.size())
                    .getResultList();
            
            for (Object[] row : additionalCategories) {
                categories.add(TopCategoriesResponse.TopCategory.builder()
                        .categoryId((java.util.UUID) row[0])
                        .categoryName((String) row[1])
                        .categoryImage((String) row[2])
                        .productCount(0L)
                        .quantitySold(0L)
                        .revenue(BigDecimal.ZERO)
                        .build());
            }
        }
        
        return TopCategoriesResponse.builder()
                .categories(categories)
                .build();
    }

    @Override
    public TopSuppliersResponse getTopSuppliers(int limit) {
        log.info("📊 [Dashboard] Lấy Top {} nhà cung cấp", limit);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        // Query các nhà cung cấp có doanh thu trong 30 ngày
        String jpql = """
            SELECT s.id as supplierId,
                   s.name as supplierName,
                   s.logoUrl as supplierLogo,
                   COUNT(DISTINCT p.id) as productCount,
                   SUM(od.quantity) as quantitySold,
                   SUM(od.sellingPricePerUnit * od.quantity) as revenue
            FROM OrderDetail od
            JOIN od.order o
            JOIN od.variant.product p
            JOIN p.supplier s
            WHERE o.orderDate >= :startDate 
              AND o.orderDate <= :endDate
              AND o.orderStatus = :status
            GROUP BY s.id, s.name, s.logoUrl
            ORDER BY revenue DESC
            """;
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("startDate", startDate.atStartOfDay())
                .setParameter("endDate", endDate.atTime(LocalTime.MAX))
                .setParameter("status", OrderStatus.DA_GIAO)
                .setMaxResults(limit)
                .getResultList();
        
        List<TopSuppliersResponse.TopSupplier> suppliers = new ArrayList<>(results.stream()
                .map(row -> TopSuppliersResponse.TopSupplier.builder()
                        .supplierId((java.util.UUID) row[0])
                        .supplierName((String) row[1])
                        .supplierLogo((String) row[2])
                        .productCount((Long) row[3])
                        .quantitySold((Long) row[4])
                        .revenue((BigDecimal) row[5])
                        .build())
                .collect(Collectors.toList()));
        
        // Nếu chưa đủ limit, bổ sung thêm các nhà cung cấp khác (không có doanh thu)
        if (suppliers.size() < limit) {
            List<java.util.UUID> existingIds = suppliers.stream()
                    .map(TopSuppliersResponse.TopSupplier::getSupplierId)
                    .collect(Collectors.toList());
            
            String fillJpql = existingIds.isEmpty()
                ? "SELECT s.id, s.name, s.logoUrl FROM Supplier s ORDER BY s.name ASC"
                : "SELECT s.id, s.name, s.logoUrl FROM Supplier s WHERE s.id NOT IN :existingIds ORDER BY s.name ASC";
            
            var query = entityManager.createQuery(fillJpql, Object[].class);
            if (!existingIds.isEmpty()) {
                query.setParameter("existingIds", existingIds);
            }
            
            List<Object[]> additionalSuppliers = query
                    .setMaxResults(limit - suppliers.size())
                    .getResultList();
            
            for (Object[] row : additionalSuppliers) {
                suppliers.add(TopSuppliersResponse.TopSupplier.builder()
                        .supplierId((java.util.UUID) row[0])
                        .supplierName((String) row[1])
                        .supplierLogo((String) row[2])
                        .productCount(0L)
                        .quantitySold(0L)
                        .revenue(BigDecimal.ZERO)
                        .build());
            }
        }
        
        return TopSuppliersResponse.builder()
                .suppliers(suppliers)
                .build();
    }
    
    // ==================== HELPER METHODS ====================
    
    private BigDecimal calculateRevenue(LocalDateTime start, LocalDateTime end, List<OrderStatus> statuses) {
        String jpql = """
            SELECT COALESCE(SUM(o.totalPayment), 0)
            FROM Order o
            WHERE o.orderDate >= :start AND o.orderDate <= :end
              AND o.orderStatus IN :statuses
            """;
        
        return entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }
    
    private Long countOrders(LocalDateTime start, LocalDateTime end, List<OrderStatus> statuses) {
        String jpql;
        if (statuses == null || statuses.isEmpty()) {
            jpql = """
                SELECT COUNT(o)
                FROM Order o
                WHERE o.orderDate >= :start AND o.orderDate <= :end
                """;
            return entityManager.createQuery(jpql, Long.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } else {
            jpql = """
                SELECT COUNT(o)
                FROM Order o
                WHERE o.orderDate >= :start AND o.orderDate <= :end
                  AND o.orderStatus IN :statuses
                """;
            return entityManager.createQuery(jpql, Long.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .setParameter("statuses", statuses)
                    .getSingleResult();
        }
    }
    
    private Long countNewCustomers(LocalDateTime start, LocalDateTime end) {
        String jpql = """
            SELECT COUNT(DISTINCT u)
            FROM User u
            JOIN u.userRoles ur
            WHERE u.createdAt >= :start AND u.createdAt <= :end
              AND ur.role = :role
              AND ur.active = true
            """;
        
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("role", Role.CUSTOMER)
                .getSingleResult();
    }
    
    private Double calculateAverageRating(LocalDateTime start, LocalDateTime end) {
        String jpql = """
            SELECT COALESCE(AVG(pr.rating), 0.0)
            FROM ProductReview pr
            WHERE pr.reviewTime >= :start AND pr.reviewTime <= :end
              AND pr.isApproved = true
            """;
        
        Double result = entityManager.createQuery(jpql, Double.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        
        return Math.round(result * 10.0) / 10.0;
    }
    
    private Double calculateTrend(Number current, Number previous) {
        if (previous == null || previous.doubleValue() == 0) {
            return current != null && current.doubleValue() > 0 ? 100.0 : 0.0;
        }
        double trend = ((current.doubleValue() - previous.doubleValue()) / previous.doubleValue()) * 100;
        return Math.round(trend * 10.0) / 10.0;
    }
    
    private Double calculateTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal diff = current.subtract(previous);
        BigDecimal trend = diff.divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return Math.round(trend.doubleValue() * 10.0) / 10.0;
    }
    
    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case DANG_CHO -> "Đang chờ";
            case DA_XAC_NHAN -> "Đã xác nhận";
            case DANG_GIAO -> "Đang giao";
            case DA_GIAO -> "Đã giao";
            case YEU_CAU_TRA_HANG -> "Yêu cầu trả hàng";
            case TRA_HANG_THANH_CONG -> "Trả hàng thành công";
            case TRA_HANG_THAT_BAI -> "Trả hàng thất bại";
            case DA_HUY -> "Đã hủy";
        };
    }
    
    private String getStatusColor(OrderStatus status) {
        return switch (status) {
            case DANG_CHO -> "#FFA500";         // Orange
            case DA_XAC_NHAN -> "#2196F3";      // Blue
            case DANG_GIAO -> "#9C27B0";        // Purple
            case DA_GIAO -> "#4CAF50";          // Green
            case YEU_CAU_TRA_HANG -> "#FF9800"; // Amber
            case TRA_HANG_THANH_CONG -> "#00BCD4"; // Cyan
            case TRA_HANG_THAT_BAI -> "#F44336";   // Red
            case DA_HUY -> "#9E9E9E";           // Grey
        };
    }
}

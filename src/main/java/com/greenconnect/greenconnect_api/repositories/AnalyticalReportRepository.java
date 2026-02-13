package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Custom Repository cho Analytical Report Queries
 * Chứa các query phức tạp cho báo cáo phân tích
 */
@Repository
public class AnalyticalReportRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Helper method to safely convert Object (byte[] or String) to String
     * MySQL driver sometimes returns UUID/VARCHAR as byte[] instead of String
     * Returns empty string "" if null to prevent Map.of() NullPointerException
     */
    private String safeToString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String str) return str;
        if (obj instanceof byte[] bytes) return new String(bytes);
        return obj.toString();
    }
    
    /**
     * Helper method to safely convert Object to Double
     * Returns 0.0 if null to prevent Map.of() NullPointerException
     */
    private Double safeDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number num) return num.doubleValue();
        return 0.0;
    }
    
    /**
     * Helper method to safely convert Object to Long
     * Returns 0L if null to prevent Map.of() NullPointerException
     */
    private Long safeLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number num) return num.longValue();
        return 0L;
    }
    
    // ==================== OVERVIEW TAB QUERIES ====================
    
    /**
     * Lấy tổng doanh thu và số đơn hàng trong khoảng thời gian
     * Chỉ tính đơn đã thanh toán (DA_THANH_TOAN) và trạng thái hợp lệ
     */
    public Map<String, Object> getTotalRevenueAndOrders(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                COALESCE(SUM(o.total_payment), 0) as totalRevenue,
                COUNT(o.id) as totalOrders
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            """;
        
        Object[] result = (Object[]) entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult();
        
        Map<String, Object> map = new HashMap<>();
        map.put("totalRevenue", safeDouble(result[0]));
        map.put("totalOrders", safeLong(result[1]));
        return map;
    }
    
    /**
     * Lấy dữ liệu doanh thu & số đơn theo tháng (cho biểu đồ)
     * Group by YEAR-MONTH
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMonthlyRevenueData(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                DATE_FORMAT(o.order_date, '%Y-%m') as yearMonth,
                MONTH(o.order_date) as month,
                COALESCE(SUM(o.total_payment), 0) as revenue,
                COUNT(o.id) as orderCount
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            GROUP BY DATE_FORMAT(o.order_date, '%Y-%m'), MONTH(o.order_date)
            ORDER BY yearMonth ASC
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
        
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("yearMonth", safeToString(row[0]));
                map.put("month", safeLong(row[1]));
                map.put("revenue", safeDouble(row[2]));
                map.put("orderCount", safeLong(row[3]));
                return map;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy dữ liệu doanh thu & số đơn theo ngày (cho bảng chi tiết)
     * Group by DATE
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDailyRevenueData(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                DATE(o.order_date) as orderDate,
                COALESCE(SUM(o.total_payment), 0) as revenue,
                COUNT(o.id) as orderCount
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            GROUP BY DATE(o.order_date)
            ORDER BY orderDate ASC
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
        
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("orderDate", row[0] != null ? row[0] : "");
                map.put("revenue", safeDouble(row[1]));
                map.put("orderCount", safeLong(row[2]));
                return map;
            })
            .collect(Collectors.toList());
    }
    
    // ==================== PRODUCT TAB QUERIES ====================
    
    /**
     * Lấy Top N sản phẩm bán chạy theo doanh thu
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        String sql = """
            SELECT 
                CAST(p.id AS CHAR) as productId,
                p.name as productName,
                p.main_image_url as productImage,
                c.name as categoryName,
                COALESCE(SUM(od.selling_price_per_unit * od.quantity), 0) as revenue,
                COALESCE(SUM(od.quantity), 0) as quantitySold,
                p.average_rating as averageRating
            FROM order_details od
            INNER JOIN product_variants pv ON od.product_variant_id = pv.id
            INNER JOIN products p ON pv.product_id = p.id
            INNER JOIN categories c ON p.category_id = c.id
            INNER JOIN orders o ON od.order_id = o.id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            GROUP BY p.id, p.name, p.main_image_url, c.name, p.average_rating
            ORDER BY revenue DESC
            LIMIT :limit
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", limit)
            .getResultList();
        
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", safeToString(row[0]));
                map.put("productName", safeToString(row[1]));
                map.put("productImage", safeToString(row[2]));
                map.put("categoryName", safeToString(row[3]));
                map.put("revenue", safeDouble(row[4]));
                map.put("quantitySold", safeLong(row[5]));
                map.put("averageRating", safeDouble(row[6]));
                return map;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Lấy sản phẩm có tỷ lệ hoàn trả cao
     * Tính từ các đơn có trạng thái: DA_HUY, TRA_HANG_THANH_CONG
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopReturnedProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        String sql = """
            SELECT 
                CAST(p.id AS CHAR) as productId,
                p.name as productName,
                p.main_image_url as productImage,
                COUNT(DISTINCT CASE WHEN o.order_status IN ('DA_HUY', 'TRA_HANG_THANH_CONG') THEN o.id END) as returnCount,
                COUNT(DISTINCT o.id) as totalOrders,
                ROUND(
                    (COUNT(DISTINCT CASE WHEN o.order_status IN ('DA_HUY', 'TRA_HANG_THANH_CONG') THEN o.id END) * 100.0) 
                    / NULLIF(COUNT(DISTINCT o.id), 0), 
                    2
                ) as returnRate,
                'Không đúng mô tả' as topReturnReason
            FROM order_details od
            INNER JOIN product_variants pv ON od.product_variant_id = pv.id
            INNER JOIN products p ON pv.product_id = p.id
            INNER JOIN orders o ON od.order_id = o.id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            GROUP BY p.id, p.name, p.main_image_url
            HAVING returnCount > 0
            ORDER BY returnRate DESC, returnCount DESC
            LIMIT :limit
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", limit)
            .getResultList();
        
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", safeToString(row[0]));
                map.put("productName", safeToString(row[1]));
                map.put("productImage", safeToString(row[2]));
                map.put("returnCount", safeLong(row[3]));
                map.put("totalOrders", safeLong(row[4]));
                map.put("returnRate", safeDouble(row[5]));
                map.put("topReturnReason", safeToString(row[6]));
                return map;
            })
            .collect(Collectors.toList());
    }
    
    // ==================== CUSTOMER TAB QUERIES ====================
    
    /**
     * Đếm số khách hàng theo tỉnh/thành phố (từ Address)
     * Sử dụng provinceName63 (chuẩn 63 tỉnh)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCustomersByProvince(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                COALESCE(a.province_name_63, 'Khác') as province,
                COUNT(DISTINCT u.id) as customerCount
            FROM users u
            INNER JOIN addresses a ON u.id = a.user_id
            INNER JOIN orders o ON u.id = o.user_id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND a.province_name_63 IS NOT NULL
            GROUP BY a.province_name_63
            ORDER BY customerCount DESC
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
        
        // Convert Object[] to Map<String, Object>
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("province", safeToString(row[0]));
                map.put("customerCount", safeLong(row[1]));
                return map;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Đếm tổng số khách hàng có đơn hàng trong khoảng thời gian
     */
    public Long getTotalCustomersInRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT COUNT(DISTINCT o.user_id) as totalCustomers
            FROM orders o
            WHERE o.order_date BETWEEN :startDate AND :endDate
            """;
        
        return ((Number) entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult()).longValue();
    }
    
    /**
     * Đếm số khách hàng MỚI trong khoảng thời gian
     * Khách hàng mới = created_at trong khoảng thời gian
     */
    public Long getNewCustomersInRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT COUNT(DISTINCT u.id) as newCustomers
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            WHERE u.created_at BETWEEN :startDate AND :endDate
            AND ur.role = 'CUSTOMER'
            """;
        
        return ((Number) entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult()).longValue();
    }
    
    /**
     * Đếm tổng số khách hàng trong toàn bộ hệ thống (có role CUSTOMER)
     * Dùng để tính tỷ lệ % khách hàng mới
     */
    public Long getTotalCustomersInSystem() {
        String sql = """
            SELECT COUNT(DISTINCT u.id) as totalCustomers
            FROM users u
            INNER JOIN user_roles ur ON u.id = ur.user_id
            WHERE ur.role = 'CUSTOMER'
            """;
        
        return ((Number) entityManager.createNativeQuery(sql)
            .getSingleResult()).longValue();
    }
    
    /**
     * Lấy Top N khách hàng VIP (chi tiêu cao nhất)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTopVipCustomers(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        String sql = """
            SELECT 
                CAST(u.id AS CHAR) as customerId,
                u.full_name as name,
                u.email as email,
                COALESCE(SUM(o.total_payment), 0) as totalSpent,
                COUNT(o.id) as orderCount,
                COALESCE(AVG(pr.rating), 0) as averageRating
            FROM users u
            INNER JOIN orders o ON u.id = o.user_id
            LEFT JOIN order_details od ON o.id = od.order_id
            LEFT JOIN product_reviews pr ON od.id = pr.order_detail_id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            GROUP BY u.id, u.full_name, u.email
            ORDER BY totalSpent DESC
            LIMIT :limit
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", limit)
            .getResultList();
        
        // Convert Object[] to Map<String, Object>
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("customerId", safeToString(row[0]));
                map.put("name", safeToString(row[1]));
                map.put("email", safeToString(row[2]));
                map.put("totalSpent", safeDouble(row[3]));
                map.put("orderCount", safeLong(row[4]));
                map.put("averageRating", safeDouble(row[5]));
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách VIP customers với phân trang (page size = 10)
     * Sắp xếp theo totalSpent DESC như logic hiện tại
     */
    public List<Map<String, Object>> getVipCustomersPaginated(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            int page,
            int size
    ) {
        String sql = """
            SELECT 
                CAST(u.id AS CHAR) as customerId,
                u.full_name as name,
                u.email as email,
                COALESCE(SUM(o.total_payment), 0) as totalSpent,
                COUNT(o.id) as orderCount,
                COALESCE(AVG(pr.rating), 0) as averageRating
            FROM users u
            INNER JOIN orders o ON u.id = o.user_id
            LEFT JOIN order_details od ON o.id = od.order_id
            LEFT JOIN product_reviews pr ON od.id = pr.order_detail_id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            GROUP BY u.id, u.full_name, u.email
            ORDER BY totalSpent DESC
            LIMIT :limit OFFSET :offset
            """;
        
        List<Object[]> results = entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", size)
            .setParameter("offset", page * size)
            .getResultList();
        
        return results.stream()
            .map(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("customerId", safeToString(row[0]));
                map.put("name", safeToString(row[1]));
                map.put("email", safeToString(row[2]));
                map.put("totalSpent", safeDouble(row[3]));
                map.put("orderCount", safeLong(row[4]));
                map.put("averageRating", safeDouble(row[5]));
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Đếm tổng số VIP customers trong khoảng thời gian
     */
    public long countVipCustomers(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT COUNT(DISTINCT u.id)
            FROM users u
            INNER JOIN orders o ON u.id = o.user_id
            WHERE o.order_date BETWEEN :startDate AND :endDate
            AND o.payment_status = 'DA_THANH_TOAN'
            AND o.order_status IN ('DANG_XU_LY', 'DANG_GIAO', 'DA_GIAO')
            """;
        
        return ((Number) entityManager.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult()).longValue();
    }
}

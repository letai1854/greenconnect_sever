package com.greenconnect.greenconnect_api.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Order;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.PaymentMethod;
import com.greenconnect.greenconnect_api.enums.PaymentStatus;

/**
 * Repository interface cho thực thể Order.
 * 
 * <p>Quản lý các đơn hàng trong hệ thống. Cung cấp các phương thức để truy vấn
 * danh sách đơn hàng cho cả người dùng và quản trị viên, với các chức năng
 * phân trang và lọc mạnh mẽ.</p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    // Find by order code
    Optional<Order> findByOrderCode(String orderCode);
    
    // Find user orders ordered by date
    Page<Order> findByUserIdOrderByOrderDateDesc(UUID userId, Pageable pageable);
    
    // REMOVED: delivery_address_id field no longer exists in Order entity
    // Page<Order> findByDeliveryAddressIdOrderByOrderDateDesc(UUID deliveryAddressId, Pageable pageable);
    
    // Find orders by rank points range
    Page<Order> findByRankPointsGreaterThanOrderByOrderDateDesc(BigDecimal rankPoints, Pageable pageable);

    /**
     * Tìm kiếm một đơn hàng dựa trên mã code của nó.
     * <p>Mã code là một chuỗi định danh duy nhất, thân thiện với người dùng (ví dụ: "GRC-10001")
     * thay vì UUID khó nhớ. Hữu ích cho việc tra cứu nhanh.</p>
     *
    //  * @param code Mã code của đơn hàng.
    //  * @return một đối tượng {@link Optional} chứa {@link Order} nếu tìm thấy.
    //  */
    // Optional<Order> findByCode(String code);

    // /**
    //  * Lấy danh sách các đơn hàng của một người dùng cụ thể, có phân trang.
    //  * <p>Đây là phương thức chính cho chức năng "Lịch sử đơn hàng" của người dùng.
    //  * Kết quả được sắp xếp theo ngày tạo mới nhất để hiển thị các đơn hàng gần đây
    //  * lên đầu.</p>
    //  *
    //  * @param userId   ID của người dùng.
    //  * @param pageable Thông tin phân trang.
    //  * @return một trang (Page) chứa danh sách các đơn hàng của người dùng đó.
    //  */
    // Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // /**
    //  * Lấy một đơn hàng cụ thể của một người dùng cụ thể.
    //  * <p>Dùng để đảm bảo người dùng chỉ có thể xem chi tiết đơn hàng của chính họ,
    //  * tăng cường bảo mật.</p>
    //  *
    //  * @param id     ID của đơn hàng.
    //  * @param userId ID của người dùng.
    //  * @return một đối tượng {@link Optional} chứa {@link Order} nếu đơn hàng tồn tại và thuộc về người dùng.
    //  */
    // Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    /*
     * LƯU Ý VỀ TÌM KIẾM VÀ LỌC CHO ADMIN:
     *
     * Việc kế thừa JpaSpecificationExecutor cho phép chúng ta xây dựng các bộ lọc đơn hàng
     * phức tạp trong lớp OrderService. Ví dụ, Admin có thể muốn lọc đơn hàng theo:
     *   - Trạng thái (PENDING, PROCESSING...).
     *   - Khoảng thời gian đặt hàng.
     *   - Người dùng cụ thể.
     *   - Phương thức thanh toán...
     *
     * Phương thức được sử dụng sẽ là: findAll(Specification<Order> spec, Pageable pageable)
     */
    
    /**
     * Tìm các đơn hàng VNPay quá hạn (chưa thanh toán sau thời gian quy định)
     * 
     * @param paymentMethod Phương thức thanh toán (VIETQR)
     * @param paymentStatus Trạng thái thanh toán (CHUA_THANH_TOAN)
     * @param orderStatus Trạng thái đơn hàng (DANG_CHO)
     * @param expiryTime Thời điểm hết hạn (LocalDateTime)
     * @return Danh sách đơn hàng quá hạn
     */
    @Query("SELECT o FROM Order o WHERE o.paymentMethod = :paymentMethod " +
           "AND o.paymentStatus = :paymentStatus " +
           "AND o.orderStatus = :orderStatus " +
           "AND o.orderDate < :expiryTime")
    List<Order> findExpiredVnpayOrders(
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("expiryTime") LocalDateTime expiryTime
    );
    
    /**
     * Lấy tất cả đơn hàng trong khoảng thời gian, sắp xếp theo ngày tạo mới nhất
     */
    Page<Order> findByOrderDateBetweenOrderByOrderDateDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng theo trạng thái đơn hàng
     */
    Page<Order> findByOrderStatusOrderByOrderDateDesc(
            OrderStatus orderStatus, 
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng theo trạng thái thanh toán
     */
    Page<Order> findByPaymentStatusOrderByOrderDateDesc(
            PaymentStatus paymentStatus, 
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng theo khoảng thời gian và trạng thái đơn hàng
     */
    Page<Order> findByOrderDateBetweenAndOrderStatusOrderByOrderDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus orderStatus,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng theo khoảng thời gian và trạng thái thanh toán
     */
    Page<Order> findByOrderDateBetweenAndPaymentStatusOrderByOrderDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            PaymentStatus paymentStatus,
            Pageable pageable
    );
    
    /**
     * Lấy tất cả đơn hàng, sắp xếp theo ngày tạo mới nhất
     */
    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa (orderCode, recipientName, recipientPhone, deliveryAddress)
     * Hỗ trợ tìm kiếm tiếng Việt có dấu
     */
    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchAllOrders(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa và trạng thái đơn hàng
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.orderStatus = :orderStatus " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByOrderStatus(
            @Param("keyword") String keyword,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa và trạng thái thanh toán
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.paymentStatus = :paymentStatus " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByPaymentStatus(
            @Param("keyword") String keyword,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa trong khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByDateRange(
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa, trạng thái đơn hàng và khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.orderStatus = :orderStatus " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByOrderStatusAndDateRange(
            @Param("keyword") String keyword,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa, trạng thái thanh toán và khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.paymentStatus = :paymentStatus " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByPaymentStatusAndDateRange(
            @Param("keyword") String keyword,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa và nhiều trạng thái đơn hàng (dùng cho DA_HUY)
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.orderStatus IN :statuses " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByOrderStatusIn(
            @Param("keyword") String keyword,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable
    );
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa, nhiều trạng thái đơn hàng và khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.deliveryAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND o.orderStatus IN :statuses " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> searchOrdersByOrderStatusInAndDateRange(
            @Param("keyword") String keyword,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    // ========== USER ORDER FILTERING ==========
    
    /**
     * Lấy đơn hàng của user theo khoảng thời gian và trạng thái đơn hàng
     */
    Page<Order> findByUserIdAndOrderDateBetweenAndOrderStatusOrderByOrderDateDesc(
            UUID userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus orderStatus,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user theo khoảng thời gian và trạng thái thanh toán
     */
    Page<Order> findByUserIdAndOrderDateBetweenAndPaymentStatusOrderByOrderDateDesc(
            UUID userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            PaymentStatus paymentStatus,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user trong khoảng thời gian
     */
    Page<Order> findByUserIdAndOrderDateBetweenOrderByOrderDateDesc(
            UUID userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user theo trạng thái đơn hàng
     */
    Page<Order> findByUserIdAndOrderStatusOrderByOrderDateDesc(
            UUID userId,
            OrderStatus orderStatus,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user theo trạng thái thanh toán
     */
    Page<Order> findByUserIdAndPaymentStatusOrderByOrderDateDesc(
            UUID userId,
            PaymentStatus paymentStatus,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng có nhiều trạng thái (dùng cho DA_HUY - bao gồm cả TRA_HANG)
     */
    Page<Order> findByOrderStatusInOrderByOrderDateDesc(
            List<OrderStatus> statuses,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng có nhiều trạng thái trong khoảng thời gian
     */
    Page<Order> findByOrderDateBetweenAndOrderStatusInOrderByOrderDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<OrderStatus> statuses,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có trạng thái hủy (DA_HUY, TRA_HANG_THANH_CONG, TRA_HANG_THAT_BAI)
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
           "AND o.orderStatus IN :statuses " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findByUserIdAndOrderStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có trạng thái hủy trong khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
           "AND o.orderStatus IN :statuses " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findByUserIdAndOrderStatusInAndOrderDateBetween(
            @Param("userId") UUID userId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có ít nhất 1 sản phẩm chưa đánh giá
     * Đơn hàng DA_GIAO và tồn tại orderDetail không có review
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.orderDetails od " +
           "WHERE o.user.id = :userId " +
           "AND o.orderStatus = 'DA_GIAO' " +
           "AND NOT EXISTS (SELECT 1 FROM ProductReview pr WHERE pr.orderDetail.id = od.id) " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findUserOrdersWithoutReviews(
            @Param("userId") UUID userId,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có ít nhất 1 sản phẩm chưa đánh giá trong khoảng thời gian
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.orderDetails od " +
           "WHERE o.user.id = :userId " +
           "AND o.orderStatus = 'DA_GIAO' " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "AND NOT EXISTS (SELECT 1 FROM ProductReview pr WHERE pr.orderDetail.id = od.id) " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findUserOrdersWithoutReviewsByDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có ít nhất 1 sản phẩm đã đánh giá
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderDetails od JOIN ProductReview pr ON od.id = pr.orderDetail.id " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findUserOrdersWithReviews(
            @Param("userId") UUID userId,
            Pageable pageable
    );
    
    /**
     * Lấy đơn hàng của user có ít nhất 1 sản phẩm đã đánh giá trong khoảng thời gian
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderDetails od JOIN ProductReview pr ON od.id = pr.orderDetail.id " +
           "WHERE o.user.id = :userId " +
           "AND o.orderDate BETWEEN :startDate AND :endDate " +
           "ORDER BY o.orderDate DESC")
    Page<Order> findUserOrdersWithReviewsByDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
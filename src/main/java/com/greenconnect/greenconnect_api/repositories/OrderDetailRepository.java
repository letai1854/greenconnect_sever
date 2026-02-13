package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.OrderDetail;
import com.greenconnect.greenconnect_api.enums.OrderStatus;

/**
 * Repository interface cho thực thể OrderDetail.
 * 
 * <p>Quản lý các mục (items) chi tiết trong một đơn hàng. Mỗi bản ghi tương ứng
 * với một sản phẩm mà khách hàng đã mua.</p>
 */
@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

    /**
     * Lấy danh sách tất cả các mục chi tiết thuộc về một đơn hàng cụ thể.
     * <p>Đây là phương thức chính, được sử dụng khi cần hiển thị danh sách sản phẩm
     * đã mua trong trang "Chi tiết đơn hàng", hoặc khi xử lý các nghiệp vụ như
     * hoàn lại tồn kho khi đơn hàng bị hủy.</p>
     *
     * @param orderId ID của đơn hàng.
     * @return một danh sách (List) các đối tượng {@link OrderDetail}.
     */
    List<OrderDetail> findByOrderId(UUID orderId);
    
    /**
     * 🔍 TÌM ORDERDETAIL VỚI KIỂM TRA QUYỀN SỞ HỮU CHO REVIEW
     * Tìm OrderDetail theo ID và kiểm tra nó thuộc về user cụ thể và đơn hàng đã hoàn thành
     * Dùng cho việc verify quyền review sản phẩm
     * @param orderStatus Trạng thái đơn hàng (thường là OrderStatus.DA_GIAO)
     */
    @Query("SELECT od FROM OrderDetail od " +
           "WHERE od.id = :orderDetailId " +
           "AND od.order.user.id = :userId " +
           "AND od.order.orderStatus = :orderStatus")
    Optional<OrderDetail> findByIdAndUserIdAndOrderCompleted(@Param("orderDetailId") UUID orderDetailId, 
                                                            @Param("userId") UUID userId,
                                                            @Param("orderStatus") OrderStatus orderStatus);
    
    /**
     * 📊 LẤY DANH SÁCH SẢN PHẨM CÓ THỂ REVIEW CỦA USER
     * Lấy các OrderDetail mà user có thể review (đã hoàn thành, chưa review)
     */
    @Query("SELECT od FROM OrderDetail od " +
           "LEFT JOIN ProductReview pr ON pr.orderDetail.id = od.id " +
           "WHERE od.order.user.id = :userId " +
           "AND od.order.orderStatus = :orderStatus " +
           "AND pr.id IS NULL " +
           "ORDER BY od.order.orderDate DESC")
    List<OrderDetail> findReviewableOrderDetailsByUserId(@Param("userId") UUID userId,
                                                        @Param("orderStatus") OrderStatus orderStatus);
}
package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.OrderStatusHistory;

/**
 * Repository interface cho thực thể OrderStatusHistory.
 * 
 * <p>Hoạt động như một cuốn "sổ nhật ký", ghi lại tất cả các lần thay đổi trạng thái
 * của một đơn hàng. Dữ liệu này rất quan trọng cho việc theo dõi, gỡ lỗi và cung cấp
 * thông tin chi tiết về hành trình của đơn hàng cho cả khách hàng và admin.</p>
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    /**
     * Lấy toàn bộ lịch sử thay đổi trạng thái của một đơn hàng cụ thể,
     * sắp xếp theo thời gian tạo để hiển thị theo đúng thứ tự.
     * <p>Đây là phương thức chính, được sử dụng trong trang "Theo dõi đơn hàng"
     * để hiển thị các mốc thời gian như: "Đã đặt hàng", "Đã xác nhận", "Đang giao"...</p>
     *
     * @param orderId ID của đơn hàng cần xem lịch sử.
     * @return một danh sách (List) các bản ghi lịch sử, đã được sắp xếp.
     */
    List<OrderStatusHistory> findByOrderIdOrderByUpdatedTimeAsc(UUID orderId);

    /**
     * Kiểm tra xem đã có bản ghi lịch sử với status cụ thể cho đơn hàng chưa.
     * Dùng để tránh trùng lặp khi cộng điểm tích lũy.
     *
     * @param orderId ID của đơn hàng
     * @param status Status cần kiểm tra (vd: "LOYALTY_POINTS_AWARDED")
     * @return true nếu đã tồn tại, false nếu chưa
     */
    boolean existsByOrderIdAndStatus(UUID orderId, String status);

}
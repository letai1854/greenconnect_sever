package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRequest;
import com.greenconnect.greenconnect_api.dtos.response.OrderResponse;
import com.greenconnect.greenconnect_api.dtos.response.OrderStatusHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.VnpayPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrderService {
    
    /**
     * Tạo đơn hàng mới với thanh toán tiền mặt
     * Backend chỉ lưu thông tin, không tính toán lại giá
     */
    OrderResponse createCashOrder(UUID userId, CreateOrderRequest request);
    
    /**
     * Tạo đơn hàng với thanh toán VNPay
     * Trả về URL thanh toán VNPay
     */
    VnpayPaymentResponse createVnpayOrder(UUID userId, CreateOrderRequest request, String clientIpAddress);
    
    /**
     * Xử lý callback từ VNPay (IPN - Instant Payment Notification)
     * Đây là nơi server VNPay gọi để thông báo kết quả thanh toán
     */
    boolean handleVnpayCallback(Map<String, String> vnpayParams);
    
    /**
     * Xử lý return URL từ VNPay (User được redirect về)
     * Xác thực và trả về kết quả cuối cùng cho frontend
     */
    OrderResponse handleVnpayReturn(Map<String, String> vnpayParams);
    
    /**
     * Lấy thông tin đơn hàng theo ID
     */
    OrderResponse getOrderById(UUID orderId);
    
    /**
     * Lấy danh sách đơn hàng của user
     */
    Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable);
    
    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng theo ID
     * (Để frontend gọi confirm lại sau khi return từ VNPay)
     */
    OrderResponse checkOrderPaymentStatus(UUID orderId);
    
    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng theo orderCode
     * (Dành cho VNPay polling - Frontend nhận vnp_TxnRef từ return URL)
     */
    OrderResponse checkOrderPaymentStatusByCode(String orderCode);
    
    /**
     * Cập nhật trạng thái đơn hàng
     * Tự động tạo OrderStatusHistory khi cập nhật
     */
    OrderResponse updateOrderStatus(UUID orderId, String newStatus, String note);
    
    /**
     * Lấy lịch sử trạng thái của đơn hàng
     */
    List<OrderStatusHistoryResponse> getOrderStatusHistory(UUID orderId);
    
    /**
     * Cập nhật trạng thái thanh toán COD
     * Dành cho shipper xác nhận đã nhận tiền từ khách hàng
     */
    OrderResponse updateCodPaymentStatus(UUID orderId, boolean isPaid, String note);
    
    /**
     * Hủy các đơn hàng VNPay quá hạn (chưa thanh toán sau 15 phút)
     * Được gọi bởi Scheduler hoặc Admin manual trigger
     * 
     * @return Số lượng đơn hàng đã bị hủy
     */
    int cancelExpiredVnpayOrders();
    
    /**
     * Lấy danh sách đơn hàng với phân trang và lọc theo ngày, trạng thái
     * 
     * @param dateFilter Lọc theo ngày: "all", "today", "this_week", "this_month"
     * @param statusFilter Lọc theo trạng thái đơn hàng hoặc thanh toán: "all", "DANG_CHO", "DA_XAC_NHAN", etc.
     * @param pageable Thông tin phân trang
     * @return Page chứa danh sách đơn hàng
     */
    Page<OrderResponse> getOrdersWithFilter(String dateFilter, String statusFilter, Pageable pageable);
    
    /**
     * Tìm kiếm đơn hàng theo từ khóa với phân trang - Hỗ trợ tiếng Việt có dấu
     * 
     * @param keyword Từ khóa tìm kiếm (orderCode, recipientName, recipientPhone, deliveryAddress)
     * @param tab Trạng thái đơn hàng: all, dang_cho, da_xac_nhan, dang_giao, da_giao, da_huy, etc.
     * @param dateFilter Lọc theo ngày: all, today, this_week, this_month
     * @param page Số trang (mặc định 0)
     * @param size Số lượng đơn hàng mỗi trang (mặc định 20)
     * @return Page chứa danh sách đơn hàng tìm được
     */
    Page<OrderResponse> searchOrders(String keyword, String tab, String dateFilter, int page, int size);
    
    /**
     * Lấy thông tin chi tiết đơn hàng (bao gồm Order, OrderDetails, OrderStatusHistory)
     * 
     * @param orderId ID của đơn hàng
     * @return OrderResponse với đầy đủ thông tin
     */
    OrderResponse getOrderDetail(UUID orderId);
    
    /**
     * Cập nhật trạng thái đơn hàng với logic cashback và cập nhật tổng tiền user
     * - Khi trạng thái = DA_GIAO: Cộng loyaltyPoints và totalPaymentAmount cho user
     * - Khi trạng thái = DA_HUY hoặc TRA_HANG_THANH_CONG: Trừ ngược lại
     * 
     * @param orderId ID của đơn hàng
     * @param newStatus Trạng thái mới
     * @param note Ghi chú
     * @return OrderResponse với trạng thái đã cập nhật
     */
    OrderResponse updateOrderStatusWithCashback(UUID orderId, String newStatus, String note);
    
    /**
     * Cập nhật trạng thái thanh toán của đơn hàng
     * Admin có thể thay đổi trạng thái thanh toán thủ công (CHUA_THANH_TOAN, DA_THANH_TOAN, THAT_BAI)
     * 
     * @param orderId ID của đơn hàng
     * @param newPaymentStatus Trạng thái thanh toán mới
     * @param note Ghi chú
     * @return OrderResponse với trạng thái thanh toán đã cập nhật
     */
    OrderResponse updatePaymentStatus(UUID orderId, String newPaymentStatus, String note);
    
    /**
     * Hủy đơn hàng - User tự hủy đơn hàng của mình
     * Chỉ được hủy khi đơn hàng ở trạng thái DANG_CHO (chờ xác nhận)
     * Tự động rollback: loyalty points, stock, voucher
     * 
     * @param orderId ID của đơn hàng
     * @param userId ID của user (để kiểm tra quyền sở hữu)
     * @param reason Lý do hủy đơn
     * @return OrderResponse với trạng thái DA_HUY
     */
    OrderResponse cancelOrder(UUID orderId, UUID userId, String reason);
    
    /**
     * User xác nhận đã nhận hàng
     * Chuyển trạng thái từ DANG_GIAO → DA_GIAO
     * Tự động cộng điểm tích lũy nếu đã thanh toán
     * 
     * @param orderId ID của đơn hàng
     * @param userId ID của user (để kiểm tra quyền sở hữu)
     * @return OrderResponse với trạng thái DA_GIAO và thông tin điểm đã cộng
     */
    OrderResponse confirmOrderReceived(UUID orderId, UUID userId);
    
    /**
     * User yêu cầu trả hàng
     * Chuyển trạng thái từ DA_GIAO → YEU_CAU_TRA_HANG
     * Chờ admin xử lý (TRA_HANG_THANH_CONG hoặc TRA_HANG_THAT_BAI)
     * 
     * @param orderId ID của đơn hàng
     * @param userId ID của user (để kiểm tra quyền sở hữu)
     * @param reason Lý do trả hàng (bắt buộc)
     * @return OrderResponse với trạng thái YEU_CAU_TRA_HANG
     */
    OrderResponse requestReturnOrder(UUID orderId, UUID userId, String reason);
    
    /**
     * Lấy danh sách đơn hàng của user với phân trang và lọc theo ngày, trạng thái
     * Lưu ý: Khi statusFilter = "DA_HUY" sẽ load cả 3 trạng thái: DA_HUY, TRA_HANG_THANH_CONG, TRA_HANG_THAT_BAI
     * 
     * @param userId ID của user
     * @param dateFilter Lọc theo ngày: "all", "today", "this_week", "this_month"
     * @param statusFilter Lọc theo trạng thái đơn hàng hoặc thanh toán
     * @param pageable Thông tin phân trang
     * @return Page chứa danh sách đơn hàng của user
     */
    Page<OrderResponse> getUserOrdersWithFilter(UUID userId, String dateFilter, String statusFilter, Pageable pageable);
    
    /**
     * Lấy danh sách đơn hàng của user theo trạng thái đánh giá
     * - CHUA_DANH_GIA: Đơn hàng có ít nhất 1 sản phẩm chưa được đánh giá
     * - DA_DANH_GIA: Đơn hàng có ít nhất 1 sản phẩm đã được đánh giá
     * Mỗi OrderItemResponse chứa productId và hasReviewed để kiểm tra từng sản phẩm
     * 
     * @param userId ID của user
     * @param reviewStatus Trạng thái đánh giá: "CHUA_DANH_GIA" hoặc "DA_DANH_GIA"
     * @param dateFilter Lọc theo ngày: "all", "today", "this_week", "this_month"
     * @param pageable Thông tin phân trang
     * @return Page chứa danh sách đơn hàng theo trạng thái đánh giá
     */
    Page<OrderResponse> getUserOrdersByReviewStatus(UUID userId, String reviewStatus, String dateFilter, Pageable pageable);
}
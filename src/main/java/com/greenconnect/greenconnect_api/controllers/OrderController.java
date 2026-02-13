package com.greenconnect.greenconnect_api.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateOrderStatusRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.OrderResponse;
import com.greenconnect.greenconnect_api.dtos.response.OrderStatusHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.VnpayPaymentResponse;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.OrderService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Order Controller - Quản lý đơn hàng
 * 
 * <p><b>🔐 LUỒNG BẢO MẬT VNPAY:</b></p>
 * <p>1. <b>Frontend Request</b> → Server tạo đơn hàng PENDING + URL VNPay (có chữ ký)</p>
 * <p>2. <b>User thanh toán</b> → VNPay xử lý</p>
 * <p>3. <b>VNPay IPN</b> → Gọi trực tiếp server (callback) → Cập nhật DB</p>
 * <p>4. <b>VNPay Return</b> → User quay về app → Frontend check status</p>
 * <p>5. <b>Final Check</b> → Frontend gọi API confirm → Hiển thị kết quả</p>
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    /**
     * 💵 TẠO ĐƠN HÀNG THANH TOÁN TIỀN MẶT
     * 
     * <p><b>Đặc điểm:</b></p>
     * <ul>
     *   <li>✅ Đơn giản nhất - tạo và lưu ngay</li>
     *   <li>✅ Trạng thái: PENDING → Chờ xác nhận</li>
     *   <li>✅ Payment Status: PENDING → COD</li>
     * </ul>
     */
    @PostMapping("/create-cash")
    public ResponseEntity<ApiResponse<OrderResponse>> createCashOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        UUID userId = getCurrentUserId();
        log.info("🛒 Tạo đơn hàng COD - User: {}, Total: {}", userId, request.getTotalPayment());
        
        try {
            OrderResponse response = orderService.createCashOrder(userId, request);
            
            ApiResponse<OrderResponse> apiResponse = ResponseUtil.success(
                response, 
                "Tạo đơn hàng thành công! Đơn hàng sẽ được xác nhận trong thời gian sớm nhất."
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi tạo đơn hàng COD: ", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
    
    /**
     * 💳 TẠO ĐƠN HÀNG THANH TOÁN VNPAY
     * 
     * <p><b>BƯỚC 1/5 TRONG LUỒNG VNPAY:</b></p>
     * <ul>
     *   <li>✅ Tạo đơn hàng với trạng thái PENDING</li>
     *   <li>✅ Tạo URL thanh toán VNPay (có chữ ký bảo mật)</li>
     *   <li>📱 Frontend sẽ mở WebView với URL này</li>
     * </ul>
     * 
     * <p><b>⚠️ LƯU Ý BẢO MẬT:</b></p>
     * <p>Số tiền được "khóa" bằng chữ ký số - user không thể sửa đổi!</p>
     */
    @PostMapping("/create-vnpay")
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createVnpayOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        UUID userId = getCurrentUserId();
        String clientIp = getClientIpAddress(httpRequest);
        
        log.info("💳 Tạo đơn hàng VNPay - User: {}, Total: {}, IP: {}", 
                userId, request.getTotalPayment(), clientIp);
        
        try {
            VnpayPaymentResponse response = orderService.createVnpayOrder(userId, request, clientIp);
            
            ApiResponse<VnpayPaymentResponse> apiResponse = ResponseUtil.success(
                response, 
                "Tạo đơn hàng thành công! Vui lòng thanh toán để hoàn tất."
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi tạo đơn hàng VNPay: ", e);
            throw e;
        }
    }
    
    /**
     * 🔔 VNPAY IPN CALLBACK (BƯỚC 4: V → B) - NGUỒN CHÂN LÝ
     * 
     * <p><b>⚡ ENDPOINT QUAN TRỌNG NHẤT:</b></p>
     * <p>VNPay server gọi trực tiếp endpoint này để thông báo kết quả thanh toán.</p>
     * <p>User/Frontend KHÔNG thể can thiệp vào quá trình này!</p>
     * 
     * <p><b>Hành động khi SUCCESS:</b></p>
     * <ul>
     *   <li>✅ Xác thực chữ ký SHA512 từ VNPay</li>
     *   <li>✅ Cập nhật PaymentStatus = DA_THANH_TOAN</li>
     *   <li>✅ Cập nhật OrderStatus = DANG_CHO</li>
     *   <li>🔥 TRỪ STOCK ngay lập tức (deductProductStock)</li>
     *   <li>📧 Gửi email thông báo thành công</li>
     * </ul>
     * 
     * <p><b>🔒 BẢO MẬT:</b></p>
     * <ul>
     *   <li>✅ Không cần JWT authentication (VNPay server gọi)</li>
     *   <li>✅ Xác thực bằng vnp_SecureHash (SHA512)</li>
     *   <li>✅ Return code "00" = success, "99" = failed</li>
     * </ul>
     * 
     * <p><b>⚠️ QUAN TRỌNG:</b> VNPay gọi IPN bằng <b>GET</b> method, không phải POST!</p>
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<String> handleVnpayCallback(HttpServletRequest request) {
        
        log.info("🔔 Nhận VNPay IPN callback");
        log.info("📍 Request URL: {}", request.getRequestURL());
        log.info("🔗 Query String: {}", request.getQueryString());
        log.info("📝 Method: {}", request.getMethod());
        
        try {
            // Convert request parameters to Map
            Map<String, String> vnpayParams = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length > 0) {
                    vnpayParams.put(key, values[0]);
                }
            });
            
            log.info("📋 VNPay callback params: {}", vnpayParams);
            log.info("📊 Total params count: {}", vnpayParams.size());
            
            boolean success = orderService.handleVnpayCallback(vnpayParams);
            
            if (success) {
                log.info("✅ VNPay callback processed successfully");
                
                // 🔄 Track purchase in Recombee (lấy orderId từ vnp_TxnRef)
                // Note: trackPurchase in RecombeeSyncService sẽ tự query order theo code
                if (recombeeSyncService != null) {
                    try {
                        String orderCode = vnpayParams.get("vnp_TxnRef");
                        if (orderCode != null) {
                            // Track với orderCode - service sẽ tự resolve
                            recombeeSyncService.trackPurchaseByOrderCode(orderCode);
                            log.info("✅ Tracked VNPay purchase for orderCode: {}", orderCode);
                        }
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to track purchase in Recombee: {}", e.getMessage());
                    }
                }
                
                return ResponseEntity.ok("00"); // VNPay expects "00" for success
            } else {
                log.error("❌ VNPay callback processing failed");
                return ResponseEntity.ok("99"); // VNPay expects "99" for failure
            }
            
        } catch (Exception e) {
            log.error("💥 Error processing VNPay callback: ", e);
            return ResponseEntity.ok("99");
        }
    }
    
    /**
     * 🔍 KIỂM TRA TRẠNG THÁI THANH TOÁN BẰNG ORDER CODE (CHO VNPAY POLLING)
     * 
     * <p><b>Kịch bản sử dụng:</b></p>
     * <ol>
     *   <li>User thanh toán xong trên VNPay</li>
     *   <li>VNPay redirect về Frontend (http://localhost:3000/payment-result?vnp_TxnRef=ORD-xxx)</li>
     *   <li>Frontend parse vnp_TxnRef (orderCode) từ URL</li>
     *   <li>Frontend gọi API này với orderCode để lấy trạng thái từ DB</li>
     *   <li>Frontend hiển thị màn hình "Thanh toán thành công" hoặc "Thất bại"</li>
     * </ol>
     */
    @GetMapping("/code/{orderCode}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> checkPaymentStatusByCode(@PathVariable String orderCode) {
        
        log.info("🔍 [POLLING] Kiểm tra trạng thái thanh toán đơn hàng bằng orderCode: {}", orderCode);
        
        try {
            OrderResponse response = orderService.checkOrderPaymentStatusByCode(orderCode);
            
            log.info("✅ [POLLING] Found order: orderCode={}, paymentStatus={}, orderStatus={}", 
                    orderCode, response.getPaymentStatus(), response.getOrderStatus());
            
            String message = "Lấy trạng thái đơn hàng thành công";
            
            // ✅ LOG SUCCESS: Backend đã xử lý VNPay thành công
            if (response.getPaymentStatus().name().equals("DA_THANH_TOAN")) {  
                log.info("✅ VNPAY BACKEND XỬ LÝ THÀNH CÔNG - Đơn hàng: {}, Trạng thái: {}, Tổng tiền: {}", 
                    response.getOrderCode(), 
                    response.getPaymentStatus(), 
                    response.getTotalPayment());
            }
            
            ApiResponse<OrderResponse> apiResponse = ResponseUtil.success(response, message);
            
            // 🔄 TODO: REDIRECT TO FRONTEND 
            // Option 1: Return JSON (hiện tại) - Frontend tự xử lý
            // Option 2: Redirect về frontend với query params
            // Example: return ResponseEntity.status(HttpStatus.FOUND)
            //             .location(URI.create("http://localhost:3000/payment-result?status=success&orderId=" + response.getOrderCode()))
            //             .build();
            
            return ResponseEntity.ok(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi xử lý VNPay return: ", e);
            throw e;
        }
    }
    
    /**
     * 🔍 KIỂM TRA TRẠNG THÁI THANH TOÁN (BƯỚC 5/5)
     * 
     * <p><b>Frontend gọi để confirm cuối cùng:</b></p>
     * <p>"Server ơi, đơn hàng ABC hiện tại trạng thái thế nào?"</p>
     * 
     * <p>Đây là bước cuối cùng để đảm bảo UI hiển thị đúng trạng thái.</p>
     */
    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> checkPaymentStatus(@PathVariable UUID orderId) {
        
        log.info("🔍 Kiểm tra trạng thái thanh toán đơn hàng: {}", orderId);
        
        try {
            OrderResponse response = orderService.checkOrderPaymentStatus(orderId);
            
            ApiResponse<OrderResponse> apiResponse = ResponseUtil.success(
                response, 
                "Lấy trạng thái đơn hàng thành công"
            );
            
            return ResponseEntity.ok(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi kiểm tra trạng thái thanh toán: ", e);
            throw e;
        }
    }
    
    /**
     * 📋 LẤY THÔNG TIN ĐƠN HÀNG
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
        
        log.info("📋 Lấy thông tin đơn hàng: {}", orderId);
        
        try {
            OrderResponse response = orderService.getOrderById(orderId);
            
            ApiResponse<OrderResponse> apiResponse = ResponseUtil.success(
                response, 
                "Lấy thông tin đơn hàng thành công"
            );
            
            return ResponseEntity.ok(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi lấy thông tin đơn hàng: ", e);
            throw e;
        }
    }
    
    /**
     * 📚 LẤY DANH SÁCH ĐƠN HÀNG CỦA USER
     */
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(Pageable pageable) {
        
        UUID userId = getCurrentUserId();
        log.info("📚 Lấy danh sách đơn hàng của user: {}", userId);
        
        try {
            Page<OrderResponse> response = orderService.getUserOrders(userId, pageable);
            
            ApiResponse<Page<OrderResponse>> apiResponse = ResponseUtil.success(
                response, 
                "Lấy danh sách đơn hàng thành công"
            );
            
            return ResponseEntity.ok(apiResponse);
            
        } catch (Exception e) {
            log.error("❌ Lỗi lấy danh sách đơn hàng: ", e);
            throw e;
        }
    }
    
    /**
     * 📊 LẤY DANH SÁCH ĐƠN HÀNG CỦA USER VỚI LỌC THEO NGÀY VÀ TRẠNG THÁI
     * 
     * <p><b>Hỗ trợ lọc theo ngày:</b></p>
     * <ul>
     *   <li>all: Tất cả đơn hàng (mặc định)</li>
     *   <li>today: Đơn hàng hôm nay</li>
     *   <li>this_week: Đơn hàng tuần này (7 ngày gần nhất)</li>
     *   <li>this_month: Đơn hàng tháng này (30 ngày gần nhất)</li>
     * </ul>
     * 
     * <p><b>Hỗ trợ lọc theo trạng thái:</b></p>
     * <ul>
     *   <li>all: Tất cả trạng thái (mặc định)</li>
     *   <li>DANG_CHO: Đơn hàng đang chờ xác nhận</li>
     *   <li>DA_XAC_NHAN: Đơn đã xác nhận</li>
     *   <li>DANG_GIAO: Đơn đang giao hàng</li>
     *   <li>DA_GIAO: Đơn đã giao thành công</li>
     *   <li>DA_HUY: Đơn đã hủy (bao gồm cả TRA_HANG_THANH_CONG, TRA_HANG_THAT_BAI)</li>
     *   <li>CHUA_THANH_TOAN: Chưa thanh toán</li>
     *   <li>DA_THANH_TOAN: Đã thanh toán</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>/orders/my-orders-filter?page=0&size=20 - Tất cả đơn hàng</li>
     *   <li>/orders/my-orders-filter?dateFilter=today&page=0&size=20 - Đơn hôm nay</li>
     *   <li>/orders/my-orders-filter?statusFilter=DANG_CHO&page=0&size=20 - Đơn đang chờ</li>
     *   <li>/orders/my-orders-filter?statusFilter=DA_HUY&page=0&size=20 - Tất cả đơn đã hủy/trả hàng</li>
     * </ul>
     * 
     * @param dateFilter Lọc theo ngày (all, today, this_week, this_month)
     * @param statusFilter Lọc theo trạng thái (all, DANG_CHO, DA_HUY, etc.)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng đơn hàng mỗi trang (mặc định 20)
     * @return Page chứa danh sách đơn hàng của user
     */
    @GetMapping("/my-orders-filter")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrdersWithFilter(
            @RequestParam(defaultValue = "all") String dateFilter,
            @RequestParam(defaultValue = "all") String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId();
        log.info("📊 User {} fetching orders - dateFilter: {}, statusFilter: {}, page: {}, size: {}",
                userId, dateFilter, statusFilter, page, size);
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<OrderResponse> orders = orderService.getUserOrdersWithFilter(userId, dateFilter, statusFilter, pageable);
        
        return ResponseEntity.ok(ResponseUtil.success(
                orders,
                "Lấy danh sách đơn hàng thành công"
        ));
    }
    
    /**
     * 📝 LẤY DANH SÁCH ĐƠN HÀNG THEO TRẠNG THÁI ĐÁNH GIÁ
     * 
     * <p><b>Chức năng:</b></p>
     * <ul>
     *   <li>Lấy danh sách đơn hàng để user biết sản phẩm nào chưa đánh giá</li>
     *   <li>Lấy danh sách đơn hàng đã đánh giá để xem lại review</li>
     *   <li>Mỗi OrderItemResponse chứa productId và hasReviewed để kiểm tra từng sản phẩm</li>
     * </ul>
     * 
     * <p><b>Trạng thái đánh giá:</b></p>
     * <ul>
     *   <li>CHUA_DANH_GIA: Đơn hàng đã giao (DA_GIAO) và có ít nhất 1 sản phẩm chưa được đánh giá</li>
     *   <li>DA_DANH_GIA: Đơn hàng có ít nhất 1 sản phẩm đã được đánh giá</li>
     * </ul>
     * 
     * <p><b>Lọc theo thời gian:</b></p>
     * <ul>
     *   <li>all: Tất cả thời gian (mặc định)</li>
     *   <li>today: Đơn hàng hôm nay</li>
     *   <li>this_week: Đơn hàng tuần này (7 ngày gần nhất)</li>
     *   <li>this_month: Đơn hàng tháng này (30 ngày gần nhất)</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>/orders/my-orders-review?reviewStatus=CHUA_DANH_GIA&page=0&size=20 - Đơn có sản phẩm chưa đánh giá</li>
     *   <li>/orders/my-orders-review?reviewStatus=DA_DANH_GIA&dateFilter=this_month - Đơn có sản phẩm đã đánh giá tháng này</li>
     * </ul>
     * 
     * @param reviewStatus Trạng thái đánh giá (CHUA_DANH_GIA, DA_DANH_GIA)
     * @param dateFilter Lọc theo ngày (all, today, this_week, this_month)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng đơn hàng mỗi trang (mặc định 20)
     * @return Page chứa danh sách đơn hàng theo trạng thái đánh giá
     */
    @GetMapping("/my-orders-review")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrdersByReviewStatus(
            @RequestParam(defaultValue = "CHUA_DANH_GIA") String reviewStatus,
            @RequestParam(defaultValue = "all") String dateFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = getCurrentUserId();
        log.info("📝 User {} fetching orders by review status: {}, dateFilter: {}, page: {}, size: {}",
                userId, reviewStatus, dateFilter, page, size);
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<OrderResponse> orders = orderService.getUserOrdersByReviewStatus(userId, reviewStatus, dateFilter, pageable);
        
        return ResponseEntity.ok(ResponseUtil.success(
                orders,
                "Lấy danh sách đơn hàng thành công"
        ));
    }
    
    /**
     * 🗑️ HỦY ĐƠN HÀNG - USER
     * 
     * <p><b>Điều kiện:</b></p>
     * <ul>
     *   <li>✅ Chỉ được hủy đơn hàng của chính mình</li>
     *   <li>✅ Chỉ được hủy khi đơn hàng ở trạng thái DANG_CHO (chờ xác nhận)</li>
     *   <li>❌ Không được hủy khi đơn đã được xác nhận hoặc đang giao</li>
     * </ul>
     * 
     * <p><b>Logic tự động:</b></p>
     * <ul>
     *   <li>🔺 Hoàn lại loyalty points (nếu COD đã trừ khi tạo đơn)</li>
     *   <li>📦 Rollback stock sản phẩm (nếu COD đã trừ stock)</li>
     *   <li>🎫 Rollback voucher đã sử dụng</li>
     *   <li>📝 Tạo OrderStatusHistory ghi lại lý do hủy</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <pre>
     * POST /orders/{orderId}/cancel
     * {
     *   "reason": "Tôi đặt nhầm địa chỉ"
     * }
     * </pre>
     * 
     * @param orderId ID của đơn hàng cần hủy
     * @param request CancelOrderRequest chứa lý do hủy (optional)
     * @return OrderResponse với trạng thái DA_HUY
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        
        UUID userId = getCurrentUserId();
        
        log.info("🗑️ User {} đang hủy đơn hàng: {} - Lý do: {}", userId, orderId, reason);
        
        try {
            OrderResponse order = orderService.cancelOrder(orderId, userId, reason);
            return ResponseEntity.ok(ResponseUtil.success(order,
                    "Hủy đơn hàng thành công"));
        } catch (Exception e) {
            log.error("❌ Lỗi khi hủy đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseUtil.error(
                    com.greenconnect.greenconnect_api.exceptions.ErrorCode.BAD_REQUEST, 
                    e.getMessage()));
        }
    }
    
    /**
     * ✅ XÁC NHẬN ĐÃ NHẬN HÀNG - USER
     * 
     * <p><b>Điều kiện:</b></p>
     * <ul>
     *   <li>✅ Chỉ được xác nhận đơn hàng của chính mình</li>
     *   <li>✅ Chỉ được xác nhận khi đơn hàng ở trạng thái DANG_GIAO</li>
     * </ul>
     * 
     * <p><b>Logic tự động:</b></p>
     * <ul>
     *   <li>🎁 Cộng điểm tích lũy (nếu đã thanh toán - VNPay hoặc COD đã thanh toán)</li>
     *   <li>📈 Cập nhật sellNumber cho sản phẩm</li>
     *   <li>📝 Tạo OrderStatusHistory</li>
     *   <li>📢 Gửi thông báo cho admin</li>
     * </ul>
     * 
     * @param orderId ID của đơn hàng
     * @return OrderResponse với trạng thái DA_GIAO và thông tin điểm tích lũy đã cộng
     */
    @PostMapping("/{orderId}/confirm-received")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrderReceived(
            @PathVariable UUID orderId) {
        
        UUID userId = getCurrentUserId();
        
        log.info("✅ User {} xác nhận đã nhận đơn hàng: {}", userId, orderId);
        
        try {
            OrderResponse order = orderService.confirmOrderReceived(orderId, userId);
            return ResponseEntity.ok(ResponseUtil.success(order,
                    "Xác nhận đã nhận hàng thành công! Cảm ơn bạn đã mua hàng."));
        } catch (Exception e) {
            log.error("❌ Lỗi khi xác nhận nhận hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseUtil.error(
                    com.greenconnect.greenconnect_api.exceptions.ErrorCode.BAD_REQUEST, 
                    e.getMessage()));
        }
    }
    
    /**
     * 🔄 YÊU CẦU TRẢ HÀNG - USER
     * 
     * <p><b>Điều kiện:</b></p>
     * <ul>
     *   <li>✅ Chỉ được yêu cầu trả hàng đơn của chính mình</li>
     *   <li>✅ Chỉ được yêu cầu khi đơn hàng ở trạng thái DA_GIAO</li>
     * </ul>
     * 
     * <p><b>Lưu ý:</b></p>
     * <ul>
     *   <li>⏳ Đơn hàng sẽ chuyển sang YEU_CAU_TRA_HANG, chờ admin xử lý</li>
     *   <li>📢 Thông báo sẽ được gửi cho admin để xét duyệt</li>
     *   <li>💰 Điểm tích lũy và tổng tiền chưa bị trừ - chờ admin duyệt TRA_HANG_THANH_CONG</li>
     * </ul>
     * 
     * @param orderId ID của đơn hàng
     * @param reason Lý do trả hàng (bắt buộc)
     * @return OrderResponse với trạng thái YEU_CAU_TRA_HANG
     */
    @PostMapping("/{orderId}/request-return")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturnOrder(
            @PathVariable UUID orderId,
            @RequestParam String reason) {
        
        UUID userId = getCurrentUserId();
        
        log.info("🔄 User {} yêu cầu trả hàng đơn: {} - Lý do: {}", userId, orderId, reason);
        
        try {
            OrderResponse order = orderService.requestReturnOrder(orderId, userId, reason);
            return ResponseEntity.ok(ResponseUtil.success(order,
                    "Yêu cầu trả hàng đã được gửi. Vui lòng chờ admin xử lý."));
        } catch (Exception e) {
            log.error("❌ Lỗi khi yêu cầu trả hàng: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ResponseUtil.error(
                    com.greenconnect.greenconnect_api.exceptions.ErrorCode.BAD_REQUEST, 
                    e.getMessage()));
        }
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Lấy User ID từ Security Context
     */
    private UUID getCurrentUserId() {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userPrincipal.getUserId();
    }
    
    /**
     * Lấy IP address của client (cần cho VNPay)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 🔄 CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG - ADMIN
     * 
     * <p><b>⭐ LOGIC ĐẦY ĐỦ:</b></p>
     * <ul>
     *   <li>✅ Tự động xử lý loyalty points (trừ/hoàn)</li>
     *   <li>✅ Tự động xử lý totalPaymentAmount (cộng/trừ)</li>
     *   <li>✅ Tự động xử lý cashback khi DA_GIAO</li>
     *   <li>✅ Tự động rollback stock khi hủy/trả hàng</li>
     *   <li>✅ Tự động rollback voucher khi hủy/trả hàng</li>
     *   <li>✅ Tự động tạo OrderStatusHistory</li>
     * </ul>
     * 
     * <p><b>Kịch bản:</b></p>
     * <ul>
     *   <li>DANG_CHO → DA_XAC_NHAN: Xác nhận đơn hàng</li>
     *   <li>DA_XAC_NHAN → DANG_GIAO: Bắt đầu giao hàng</li>
     *   <li>DANG_GIAO → DA_GIAO: Giao thành công → Cộng cashback</li>
     *   <li>* → DA_HUY: Hủy đơn → Hoàn loyalty + trừ totalPayment + rollback stock/voucher</li>
     *   <li>DA_GIAO → TRA_HANG_*: Trả hàng → Trừ cashback + hoàn loyalty + rollback</li>
     * </ul>
     * 
     * @param orderId ID của đơn hàng
     * @param request UpdateOrderStatusRequest chứa newStatus và note
     * @return OrderResponse với trạng thái đã cập nhật
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        log.info("🔄 Admin updating order {} status to {} with note: {}", 
            orderId, request.getNewStatus(), request.getNote());

        // ⭐ Use updateOrderStatusWithCashback for full business logic
        OrderResponse order = orderService.updateOrderStatusWithCashback(
            orderId, request.getNewStatus(), request.getNote());

        return ResponseEntity.ok(ResponseUtil.success(order,
                "Cập nhật trạng thái đơn hàng thành công"));
    }
    
    /**
     * 💰 CẬP NHẬT TRẠNG THÁI THANH TOÁN COD
     * 
     * <p><b>Sử dụng:</b> Khi shipper xác nhận đã nhận tiền từ khách hàng</p>
     * <p><b>Luồng:</b> PENDING → PAID cho đơn hàng COD</p>
     * <p><b>Tự động tạo:</b> OrderStatusHistory cho việc thanh toán</p>
     * 
     * @param orderId ID của đơn hàng COD
     * @param isPaid true = đã thanh toán, false = chưa thanh toán
     * @param note Ghi chú về việc thanh toán (optional)
     * @return OrderResponse với payment status đã cập nhật
     */
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateCodPaymentStatus(
            @PathVariable UUID orderId,
            @RequestParam boolean isPaid,
            @RequestParam(required = false) String note) {
        
        log.info("Updating COD payment status for order {} - isPaid: {}, note: {}", 
                orderId, isPaid, note);
        
        OrderResponse order = orderService.updateCodPaymentStatus(orderId, isPaid, note);
        
        // 🔄 Track purchase in Recombee khi COD đã thanh toán
        if (isPaid && recombeeSyncService != null) {
            try {
                recombeeSyncService.trackPurchase(orderId);
                log.info("✅ Tracked COD purchase for order: {}", orderId);
            } catch (Exception e) {
                log.warn("⚠️ Failed to track purchase in Recombee: {}", e.getMessage());
            }
        }
        
        String message = isPaid 
            ? "Cập nhật trạng thái thanh toán thành công - Đã thanh toán" 
            : "Cập nhật trạng thái thanh toán thành công - Chưa thanh toán";
        
        return ResponseEntity.ok(ResponseUtil.success(order, message));
    }
    
    /**
     * 📋 LẤY LỊCH SỬ TRẠNG THÁI ĐƠN HÀNG
     * 
     * <p><b>Sử dụng:</b> Theo dõi journey của đơn hàng từ lúc tạo đến hoàn thành</p>
     * <p><b>Dữ liệu:</b> Tất cả các thay đổi trạng thái theo thứ tự thời gian</p>
     * 
     * @param orderId ID của đơn hàng
     * @return List các bản ghi lịch sử trạng thái
     */
    @GetMapping("/{orderId}/status-history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderStatusHistory(
            @PathVariable UUID orderId) {
        
        log.info("Getting status history for order: {}", orderId);
        
        List<OrderStatusHistoryResponse> history = orderService.getOrderStatusHistory(orderId);
        
        return ResponseEntity.ok(ResponseUtil.success(history, 
                "Lấy lịch sử trạng thái đơn hàng thành công"));
    }
    
    /**
     * 🗑️ HỦY ĐƠN HÀNG VNPAY QUÁ HẠN (ADMIN MANUAL TRIGGER)
     * 
     * <p><b>Chức năng:</b></p>
     * <ul>
     *   <li>🕐 Tự động hủy tất cả đơn VNPay chưa thanh toán quá 15 phút</li>
     *   <li>🔄 Rollback voucher (nếu có)</li>
     *   <li>⚠️ KHÔNG cần rollback stock (vì chưa bao giờ trừ)</li>
     * </ul>
     * 
     * <p><b>Lưu ý:</b> Scheduler tự động chạy mỗi 15 phút, endpoint này cho admin trigger thủ công</p>
     * 
     * @return Số lượng đơn hàng đã bị hủy
     */
    @PostMapping("/admin/cancel-expired-vnpay")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelExpiredVnpayOrders() {
        log.info("Admin manually triggering cancel expired VNPay orders");
        
        int cancelledCount = orderService.cancelExpiredVnpayOrders();
        
        Map<String, Object> result = new HashMap<>();
        result.put("cancelledOrders", cancelledCount);
        result.put("message", cancelledCount > 0 
            ? String.format("Đã hủy thành công %d đơn hàng VNPay quá hạn", cancelledCount)
            : "Không có đơn hàng VNPay nào quá hạn");
        
        return ResponseEntity.ok(ResponseUtil.success(result, 
                "Hoàn thành kiểm tra và hủy đơn hàng VNPay quá hạn"));
    }
    
    /**
     * 📊 LẤY DANH SÁCH ĐƠN HÀNG VỚI LỌC THEO NGÀY VÀ TRẠNG THÁI - ADMIN
     * 
     * <p><b>Hỗ trợ 2 tab lọc:</b></p>
     * <ul>
     *   <li><b>Tab 1 - Lọc theo ngày:</b>
     *     <ul>
     *       <li>all: Tất cả đơn hàng (mặc định)</li>
     *       <li>today: Đơn hàng hôm nay</li>
     *       <li>this_week: Đơn hàng tuần này (7 ngày gần nhất)</li>
     *       <li>this_month: Đơn hàng tháng này (30 ngày gần nhất)</li>
     *     </ul>
     *   </li>
     *   <li><b>Tab 2 - Lọc theo trạng thái:</b>
     *     <ul>
     *       <li>all: Tất cả trạng thái (mặc định)</li>
     *       <li>DANG_CHO, DA_XAC_NHAN, DANG_GIAO, DA_GIAO (OrderStatus)</li>
     *       <li>DA_HUY: Bao gồm tất cả đơn đã hủy/trả hàng (DA_HUY, TRA_HANG_THANH_CONG, TRA_HANG_THAT_BAI)</li>
     *       <li>CHUA_THANH_TOAN, DA_THANH_TOAN, THAT_BAI (PaymentStatus)</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>/orders/admin/all?page=0&size=20 - Tất cả đơn hàng</li>
     *   <li>/orders/admin/all?dateFilter=today&page=0&size=20 - Đơn hàng hôm nay</li>
     *   <li>/orders/admin/all?statusFilter=DANG_CHO&page=0&size=20 - Đơn hàng đang chờ</li>
     *   <li>/orders/admin/all?statusFilter=DA_HUY&page=0&size=20 - Tất cả đơn đã hủy/trả hàng</li>
     *   <li>/orders/admin/all?dateFilter=this_week&statusFilter=DA_THANH_TOAN&page=0&size=20 - Đơn đã thanh toán tuần này</li>
     * </ul>
     * 
     * @param dateFilter Lọc theo ngày (all, today, this_week, this_month)
     * @param statusFilter Lọc theo trạng thái OrderStatus hoặc PaymentStatus (all, DANG_CHO, DA_HUY, CHUA_THANH_TOAN, etc.)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng đơn hàng mỗi trang (mặc định 20)
     * @return Page chứa danh sách đơn hàng với thông tin phân trang
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersWithFilter(
            @RequestParam(defaultValue = "all") String dateFilter,
            @RequestParam(defaultValue = "all") String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("📊 Admin fetching orders - dateFilter: {}, statusFilter: {}, page: {}, size: {}",
                dateFilter, statusFilter, page, size);
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<OrderResponse> orders = orderService.getOrdersWithFilter(dateFilter, statusFilter, pageable);
        
        return ResponseEntity.ok(ResponseUtil.success(
                orders,
                "Lấy danh sách đơn hàng thành công"
        ));
    }
    
    /**
     * 🔍 TÌM KIẾM ĐƠN HÀNG VỚI PHÂN TRANG - ADMIN
     * 
     * <p><b>Hỗ trợ tìm kiếm theo:</b></p>
     * <ul>
     *   <li>orderCode: Mã đơn hàng (ORD-xxxxx)</li>
     *   <li>recipientName: Tên người nhận</li>
     *   <li>recipientPhone: Số điện thoại người nhận</li>
     *   <li>deliveryAddress: Địa chỉ giao hàng</li>
     * </ul>
     * 
     * <p><b>Lọc theo tab (trạng thái):</b></p>
     * <ul>
     *   <li>all: Tất cả đơn hàng</li>
     *   <li>dang_cho: Đơn hàng đang chờ xác nhận</li>
     *   <li>da_xac_nhan: Đơn đã xác nhận</li>
     *   <li>dang_giao: Đơn đang giao hàng</li>
     *   <li>da_giao: Đơn đã giao thành công</li>
     *   <li>da_huy: Đơn đã hủy</li>
     *   <li>yeu_cau_tra_hang: Đơn yêu cầu trả hàng</li>
     *   <li>tra_hang_thanh_cong: Trả hàng thành công</li>
     *   <li>tra_hang_that_bai: Trả hàng thất bại</li>
     *   <li>chua_thanh_toan: Chưa thanh toán</li>
     *   <li>da_thanh_toan: Đã thanh toán</li>
     *   <li>that_bai: Thanh toán thất bại</li>
     * </ul>
     * 
     * <p><b>Lọc theo thời gian:</b></p>
     * <ul>
     *   <li>all: Tất cả thời gian (mặc định)</li>
     *   <li>today: Đơn hàng hôm nay</li>
     *   <li>this_week: Đơn hàng tuần này (7 ngày gần nhất)</li>
     *   <li>this_month: Đơn hàng tháng này (30 ngày gần nhất)</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <ul>
     *   <li>/orders/search?keyword=ORD-12345&tab=all&dateFilter=all&page=0&size=20</li>
     *   <li>/orders/search?keyword=Nguyễn Văn A&tab=dang_cho&dateFilter=today&page=0&size=20</li>
     *   <li>/orders/search?keyword=0912345678&tab=da_thanh_toan&dateFilter=this_week&page=0&size=20</li>
     * </ul>
     * 
     * @param keyword Từ khóa tìm kiếm (tiếng Việt có dấu)
     * @param tab Trạng thái đơn hàng hoặc thanh toán
     * @param dateFilter Lọc theo thời gian (all, today, this_week, this_month)
     * @param page Số trang (mặc định 0)
     * @param size Số lượng đơn hàng mỗi trang (mặc định 20)
     * @return Page chứa danh sách đơn hàng tìm được
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrders(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "all") String dateFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("🔍 Admin searching orders - Keyword: '{}', Tab: '{}', DateFilter: '{}', Page: {}, Size: {}",
                keyword, tab, dateFilter, page, size);
        
        // Validate tab
        Page<OrderResponse> orders = orderService.searchOrders(keyword, tab, dateFilter, page, size);
        
        return ResponseEntity.ok(ResponseUtil.success(
                orders,
                "Tìm kiếm đơn hàng thành công - Tìm thấy " + orders.getTotalElements() + " kết quả"
        ));
    }
    
    /**
     * 🔍 TÌM KIẾM ĐƠN HÀNG VỚI PHÂN TRANG - ADMIN (POST METHOD)
     * 
     * <p>Alternative endpoint sử dụng POST method với Request Body</p>
     * <p>Tương tự endpoint GET /orders/search nhưng nhận dữ liệu qua body</p>
     * 
     * @param request SearchOrderRequest chứa keyword, tab, dateFilter, page, size
     * @return Page chứa danh sách đơn hàng tìm được
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> searchOrdersWithBody(
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.SearchOrderRequest request) {
        
        log.info("🔍 Admin searching orders (POST) - Keyword: '{}', Tab: '{}', DateFilter: '{}', Page: {}, Size: {}",
                request.getKeyword(), request.getTab(), request.getDateFilter(), request.getPage(), request.getSize());
        
        Page<OrderResponse> orders = orderService.searchOrders(
                request.getKeyword(),
                request.getTab(),
                request.getDateFilter() != null ? request.getDateFilter() : "all",
                request.getPage(),
                request.getSize()
        );
        
        return ResponseEntity.ok(ResponseUtil.success(
                orders,
                "Tìm kiếm đơn hàng thành công - Tìm thấy " + orders.getTotalElements() + " kết quả"
        ));
    }
    
    /**
     * 📑 LẤY THÔNG TIN CHI TIẾT ĐƠN HÀNG - ADMIN
     * 
     * <p><b>Thông tin bao gồm:</b></p>
     * <ul>
     *   <li>Order: Thông tin cơ bản đơn hàng</li>
     *   <li>OrderDetails: Danh sách sản phẩm trong đơn</li>
     *   <li>OrderStatusHistory: Lịch sử thay đổi trạng thái</li>
     *   <li>User: Thông tin khách hàng</li>
     *   <li>Vouchers: Danh sách voucher đã áp dụng</li>
     * </ul>
     * 
     * @param orderId ID của đơn hàng
     * @return OrderResponse với đầy đủ thông tin chi tiết
     */
    @GetMapping("/admin/{orderId}/detail")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(@PathVariable UUID orderId) {
        
        log.info("📑 Admin lấy thông tin chi tiết đơn hàng: {}", orderId);
        
        OrderResponse response = orderService.getOrderDetail(orderId);
        
        return ResponseEntity.ok(ResponseUtil.success(
                response,
                "Lấy thông tin chi tiết đơn hàng thành công"
        ));
    }
    
    /**
     * 🔄 CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG VỚI CASHBACK - ADMIN
     * 
     * <p><b>Logic xử lý:</b></p>
     * <ul>
     *   <li><b>DA_GIAO:</b>
     *     <ul>
     *       <li>✅ Tính cashback: < 500k: 1%, 500k-2M: 2%, > 2M: 3%</li>
     *       <li>✅ Cộng vào loyaltyPoints của user</li>
     *       <li>✅ Cộng vào totalPaymentAmount của user</li>
     *     </ul>
     *   </li>
     *   <li><b>DA_HUY / TRA_HANG_THANH_CONG:</b>
     *     <ul>
     *       <li>❌ Trừ ngược lại loyaltyPoints (nếu trước đó là DA_GIAO)</li>
     *       <li>❌ Trừ ngược lại totalPaymentAmount (nếu trước đó là DA_GIAO)</li>
     *       <li>🔄 Rollback stock sản phẩm (nếu đã thanh toán)</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <pre>
     * PUT /orders/admin/{orderId}/status-with-cashback
     * {
     *   "newStatus": "DA_GIAO",
     *   "note": "Đơn hàng đã giao thành công"
     * }
     * </pre>
     * 
     * @param orderId ID của đơn hàng
     * @param request UpdateOrderStatusRequest chứa newStatus và note
     * @return OrderResponse với trạng thái đã cập nhật
     */
    @PutMapping("/admin/{orderId}/status-with-cashback")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatusWithCashback(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        
        log.info("🔄 Admin cập nhật trạng thái với cashback - OrderId: {}, NewStatus: {}",
                orderId, request.getNewStatus());
        
        OrderResponse response = orderService.updateOrderStatusWithCashback(
                orderId, 
                request.getNewStatus(), 
                request.getNote()
        );
        
        // 🔄 Track purchase in Recombee khi đơn hàng được giao thành công (DA_GIAO)
        if ("DA_GIAO".equals(request.getNewStatus()) && recombeeSyncService != null) {
            try {
                recombeeSyncService.trackPurchase(orderId);
                log.info("✅ Tracked delivered order purchase for order: {}", orderId);
            } catch (Exception e) {
                log.warn("⚠️ Failed to track purchase in Recombee: {}", e.getMessage());
            }
        }
        
        return ResponseEntity.ok(ResponseUtil.success(
                response,
                "Cập nhật trạng thái đơn hàng thành công"
        ));
    }
    
    /**
     * 💳 CẬP NHẬT TRẠNG THÁI THANH TOÁN ĐƠN HÀNG - ADMIN
     * 
     * <p><b>Chức năng:</b></p>
     * <ul>
     *   <li>Admin có thể thay đổi trạng thái thanh toán thủ công</li>
     *   <li>Tự động tạo OrderStatusHistory khi cập nhật</li>
     *   <li>Hỗ trợ 3 trạng thái: CHUA_THANH_TOAN, DA_THANH_TOAN, THAT_BAI</li>
     * </ul>
     * 
     * <p><b>Kịch bản sử dụng:</b></p>
     * <ul>
     *   <li>Xác nhận thanh toán thủ công cho đơn COD</li>
     *   <li>Đánh dấu đơn hàng thanh toán thất bại cần xử lý</li>
     *   <li>Rollback trạng thái thanh toán khi có sai sót</li>
     * </ul>
     * 
     * <p><b>Ví dụ:</b></p>
     * <pre>
     * PUT /orders/admin/{orderId}/payment-status
     * {
     *   "newPaymentStatus": "DA_THANH_TOAN",
     *   "note": "Xác nhận đã nhận tiền COD từ shipper"
     * }
     * </pre>
     * 
     * @param orderId ID của đơn hàng
     * @param request UpdatePaymentStatusRequest chứa newPaymentStatus và note
     * @return OrderResponse với trạng thái thanh toán đã cập nhật
     */
    @PutMapping("/admin/{orderId}/payment-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ORDER_MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.UpdatePaymentStatusRequest request) {
        
        log.info("💳 Admin cập nhật trạng thái thanh toán - OrderId: {}, NewPaymentStatus: {}",
                orderId, request.getNewPaymentStatus());
        
        OrderResponse response = orderService.updatePaymentStatus(
                orderId,
                request.getNewPaymentStatus(),
                request.getNote()
        );
        
        return ResponseEntity.ok(ResponseUtil.success(
                response,
                "Cập nhật trạng thái thanh toán thành công"
        ));
    }
}
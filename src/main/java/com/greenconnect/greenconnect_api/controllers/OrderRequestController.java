package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dto.OrderRequestDTO;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import com.greenconnect.greenconnect_api.enums.RequestType;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.services.OrderRequestService;
import com.greenconnect.greenconnect_api.services.OrderService;
import com.greenconnect.greenconnect_api.services.NotificationDatabaseService;
import com.greenconnect.greenconnect_api.repositories.OrderRepository;
import com.greenconnect.greenconnect_api.repositories.OrderRequestRepository;
import com.greenconnect.greenconnect_api.entities.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/order-requests")
@RequiredArgsConstructor
@Slf4j
public class OrderRequestController {

    private final OrderRequestService orderRequestService;
    private final NotificationDatabaseService notificationDatabaseService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderRequestRepository orderRequestRepository;

    /**
     * Create a new order request
     * Only CUSTOMER, ADMIN, CUSTOMER_SUPPORT can access
     * 
     * <p><b>Lưu ý đặc biệt:</b></p>
     * <ul>
     *   <li>Nếu requestType = HOAN_TIEN và có orderId → Tự động cập nhật trạng thái đơn hàng thành YEU_CAU_TRA_HANG</li>
     *   <li>Điều kiện: Đơn hàng phải đang ở trạng thái DA_GIAO</li>
     *   <li>⚠️ Chống spam: Chỉ cho phép 1 yêu cầu HOAN_TIEN đang xử lý cho mỗi đơn hàng</li>
     * </ul>
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<OrderRequestDTO>> createOrderRequest(
            @RequestBody OrderRequestDTO orderRequestDTO) {
        
        // ⭐ CHỐNG SPAM: Kiểm tra xem đã có yêu cầu HOAN_TIEN đang xử lý cho đơn hàng này chưa
        // Sử dụng orderCode vì frontend gửi mã đơn hàng, không gửi orderId (UUID)
        if (orderRequestDTO.getRequestType() == RequestType.HOAN_TIEN && 
            orderRequestDTO.getOrderCode() != null && !orderRequestDTO.getOrderCode().trim().isEmpty()) {
            
            // Tìm order theo orderCode để lấy orderId
            Optional<Order> orderForSpamCheck = orderRepository.findByOrderCode(orderRequestDTO.getOrderCode().trim());
            
            if (orderForSpamCheck.isPresent()) {
                UUID orderId = orderForSpamCheck.get().getId();
                
                // Đếm số request HOAN_TIEN đang DANG_XU_LY cho order này
                long pendingRefundCount = orderRequestRepository.countByOrderIdAndRequestTypeAndStatus(
                    orderId,
                    RequestType.HOAN_TIEN,
                    RequestStatus.DANG_XU_LY
                );
                
                if (pendingRefundCount > 0) {
                    log.warn("⚠️ Spam detected: User đã có {} yêu cầu HOAN_TIEN đang xử lý cho đơn hàng {}", 
                        pendingRefundCount, orderRequestDTO.getOrderCode());
                    
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ResponseUtil.error(
                            ErrorCode.REFUND_REQUEST_ALREADY_EXISTS,
                            "Bạn đã có yêu cầu hoàn tiền đang được xử lý cho đơn hàng này. Vui lòng chờ phản hồi từ bộ phận hỗ trợ."
                        )
                    );
                }
            }
        }
        
        // ⭐ Nếu là yêu cầu HOAN_TIEN và có orderCode → Cập nhật trạng thái đơn hàng thành YEU_CAU_TRA_HANG
        if (orderRequestDTO.getRequestType() == RequestType.HOAN_TIEN && 
            orderRequestDTO.getOrderCode() != null && !orderRequestDTO.getOrderCode().trim().isEmpty()) {
            
            log.info("📋 [DEBUG] Bắt đầu xử lý yêu cầu HOAN_TIEN cho orderCode: {}", orderRequestDTO.getOrderCode());
            
            try {
                // Tìm order theo orderCode (mã đơn hàng) thay vì orderId (UUID)
                Optional<Order> orderOpt = orderRepository.findByOrderCode(orderRequestDTO.getOrderCode().trim());
                
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    log.info("📋 [DEBUG] Tìm thấy đơn hàng: {}, Trạng thái hiện tại: {}", 
                        order.getOrderCode(), order.getOrderStatus());
                    
                    // Chỉ cập nhật nếu đơn hàng đang ở trạng thái DA_GIAO
                    if (order.getOrderStatus() == OrderStatus.DA_GIAO) {
                        log.info("🔄 Tự động cập nhật đơn hàng {} từ DA_GIAO → YEU_CAU_TRA_HANG", order.getOrderCode());
                        
                        // Sử dụng service để cập nhật (có đầy đủ logic history, notification)
                        String returnNote = "Yêu cầu trả hàng/hoàn tiền qua form hỗ trợ. Lý do: " + 
                            (orderRequestDTO.getReason() != null ? orderRequestDTO.getReason() : "Không có lý do");
                        
                        orderService.updateOrderStatusWithCashback(
                            order.getId(), 
                            "YEU_CAU_TRA_HANG", 
                            returnNote
                        );
                        
                        log.info("✅ Đã cập nhật trạng thái đơn hàng {} thành YEU_CAU_TRA_HANG", order.getOrderCode());
                    } else {
                        log.warn("⚠️ Đơn hàng {} không ở trạng thái DA_GIAO (hiện tại: {}), không tự động chuyển YEU_CAU_TRA_HANG", 
                            order.getOrderCode(), order.getOrderStatus());
                    }
                } else {
                    log.warn("⚠️ [DEBUG] Không tìm thấy đơn hàng với orderCode: {}", orderRequestDTO.getOrderCode());
                }
            } catch (Exception e) {
                log.error("❌ Lỗi khi cập nhật trạng thái đơn hàng: {} - StackTrace: ", e.getMessage(), e);
                // Không throw exception, vẫn tiếp tục tạo request
            }
        }
        
        OrderRequestDTO createdRequest = orderRequestService.createOrderRequest(orderRequestDTO);
        
        // 📢 Tạo thông báo cho TẤT CẢ MANAGER khi có yêu cầu mới từ customer
        try {
            String notificationType = getNotificationType(createdRequest.getRequestType());
            String title = getNotificationTitleForManager(createdRequest.getRequestType(), createdRequest.getOrderCode(), createdRequest.getUserId());
            String message = getNotificationMessageForManager(createdRequest.getRequestType(), createdRequest.getOrderCode(), createdRequest.getReason());
            
            notificationDatabaseService.createNotificationForMultipleUsers(
                java.util.Arrays.asList(createdRequest.getUserId()), // userId của customer (chủ sở hữu notification)
                notificationType,
                title,
                message,
                null,
                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
            );
            log.info("✅ Đã tạo thông báo {} cho tất cả managers - Request: {}", notificationType, createdRequest.getRequestId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo thông báo: {}", e.getMessage());
        }
        
        ApiResponse<OrderRequestDTO> apiResponse = ResponseUtil.success(
            createdRequest,
            "Order request created successfully"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
    
    /**
     * Helper method để xác định loại thông báo dựa trên RequestType
     */
    private String getNotificationType(RequestType requestType) {
        return switch (requestType) {
            case HOAN_TIEN -> "hoan_tien";
            case HO_TRO -> "ho_tro";
            case TU_VAN -> "tu_van";
            case LOI -> "loi";
            case GOP_Y -> "gop_y";
            default -> "yeu_cau";
        };
    }
    
    /**
     * Helper method để tạo tiêu đề thông báo cho MANAGER
     */
    private String getNotificationTitleForManager(RequestType requestType, String orderCode, UUID userId) {
        String orderInfo = (orderCode != null && !orderCode.trim().isEmpty()) 
            ? " - " + orderCode 
            : "";
        
        return switch (requestType) {
            case HOAN_TIEN -> "Yêu cầu hoàn tiền mới" + orderInfo;
            case HO_TRO -> "Yêu cầu hỗ trợ mới" + orderInfo;
            case TU_VAN -> "Yêu cầu tư vấn mới" + orderInfo;
            case LOI -> "Báo lỗi mới" + orderInfo;
            case GOP_Y -> "Góp ý mới" + orderInfo;
            default -> "Yêu cầu mới" + orderInfo;
        };
    }
    
    /**
     * Helper method để tạo nội dung thông báo cho MANAGER
     */
    private String getNotificationMessageForManager(RequestType requestType, String orderCode, String reason) {
        String reasonText = (reason != null && !reason.trim().isEmpty()) 
            ? reason 
            : "Vui lòng xem chi tiết yêu cầu.";
        
        return reasonText;
    }

    /**
     * Update an existing order request
     * Only CUSTOMER, ADMIN, CUSTOMER_SUPPORT can access
     * Khi Admin phản hồi (reply), sẽ gửi thông báo cho customer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<OrderRequestDTO>> updateOrderRequest(
            @PathVariable UUID id,
            @RequestBody OrderRequestDTO orderRequestDTO) {
        
        OrderRequestDTO updatedRequest = orderRequestService.updateOrderRequest(id, orderRequestDTO);
        
        // 📢 GỬI THÔNG BÁO CHO CUSTOMER khi Admin phản hồi yêu cầu
        try {
            boolean hasReply = orderRequestDTO.getReply() != null && !orderRequestDTO.getReply().trim().isEmpty();
            boolean hasStatusUpdate = orderRequestDTO.getStatus() != null;
            
            // ✅ GỘP THÀNH 1 THÔNG BÁO nếu cả reply và status đều được cập nhật
            if (hasReply && hasStatusUpdate) {
                String title = getNotificationTitleForCustomer(updatedRequest.getRequestType(), updatedRequest.getRequestId());
                String message = getCombinedNotificationMessage(updatedRequest.getReply(), updatedRequest.getStatus());
                
                notificationDatabaseService.createNotificationForCustomer(
                    updatedRequest.getUserId(),
                    "phan_hoi_yeu_cau",
                    title,
                    message,
                    "/order-requests/" + updatedRequest.getId()
                );
                log.info("📢 Đã gửi thông báo phản hồi + cập nhật trạng thái cho customer: {}", updatedRequest.getUserId());
            }
            // Chỉ có reply, không có status update
            else if (hasReply) {
                String title = getNotificationTitleForCustomer(updatedRequest.getRequestType(), updatedRequest.getRequestId());
                String message = updatedRequest.getReply();
                
                notificationDatabaseService.createNotificationForCustomer(
                    updatedRequest.getUserId(),
                    "phan_hoi_yeu_cau",
                    title,
                    message,
                    "/order-requests/" + updatedRequest.getId()
                );
                log.info("📢 Đã gửi thông báo phản hồi yêu cầu cho customer: {}", updatedRequest.getUserId());
            }
            // Chỉ có status update, không có reply
            else if (hasStatusUpdate) {
                String title = getStatusUpdateTitleForCustomer(updatedRequest.getRequestType(), updatedRequest.getRequestId(), updatedRequest.getStatus());
                String message = getStatusUpdateMessageForCustomer(updatedRequest.getStatus());
                
                notificationDatabaseService.createNotificationForCustomer(
                    updatedRequest.getUserId(),
                    "trang_thai_yeu_cau",
                    title,
                    message,
                    "/order-requests/" + updatedRequest.getId()
                );
                log.info("📢 Đã gửi thông báo cập nhật trạng thái yêu cầu cho customer: {}", updatedRequest.getUserId());
            }
        } catch (Exception e) {
            log.error("❌ Lỗi gửi thông báo cho customer: {}", e.getMessage());
        }
        
        ApiResponse<OrderRequestDTO> apiResponse = ResponseUtil.success(
            updatedRequest,
            "Order request updated successfully"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Helper: Tiêu đề thông báo phản hồi cho Customer
     */
    private String getNotificationTitleForCustomer(RequestType requestType, Long requestId) {
        String requestTypeText = switch (requestType) {
            case HOAN_TIEN -> "Yêu cầu hoàn tiền";
            case HO_TRO -> "Yêu cầu hỗ trợ";
            case TU_VAN -> "Yêu cầu tư vấn";
            case LOI -> "Báo lỗi";
            case GOP_Y -> "Góp ý";
            default -> "Yêu cầu";
        };
        return requestTypeText + " #" + requestId + " đã được phản hồi";
    }
    
    /**
     * Helper: Nội dung thông báo gộp (reply + status) cho Customer
     */
    private String getCombinedNotificationMessage(String reply, RequestStatus status) {
        String statusText = "";
        if (status == RequestStatus.HOAN_THANH) {
            statusText = "\n\n✅ Trạng thái: Đã hoàn thành. Cảm ơn bạn đã liên hệ với chúng tôi!";
        } else if (status == RequestStatus.DANG_XU_LY) {
            statusText = "\n\n⏳ Trạng thái: Đang xử lý.";
        }
        return reply + statusText;
    }
    
    /**
     * Helper: Tiêu đề thông báo cập nhật trạng thái cho Customer
     */
    private String getStatusUpdateTitleForCustomer(RequestType requestType, Long requestId, RequestStatus status) {
        String requestTypeText = switch (requestType) {
            case HOAN_TIEN -> "Yêu cầu hoàn tiền";
            case HO_TRO -> "Yêu cầu hỗ trợ";
            case TU_VAN -> "Yêu cầu tư vấn";
            case LOI -> "Báo lỗi";
            case GOP_Y -> "Góp ý";
            default -> "Yêu cầu";
        };
        
        String statusText = status == RequestStatus.HOAN_THANH ? "đã hoàn thành" : "đang được xử lý";
        return requestTypeText + " #" + requestId + " " + statusText;
    }
    
    /**
     * Helper: Nội dung thông báo cập nhật trạng thái cho Customer
     */
    private String getStatusUpdateMessageForCustomer(RequestStatus status) {
        return switch (status) {
            case HOAN_THANH -> "Yêu cầu của bạn đã được xử lý hoàn tất. Cảm ơn bạn đã liên hệ với chúng tôi!";
            case DANG_XU_LY -> "Yêu cầu của bạn đang được nhân viên hỗ trợ xử lý. Vui lòng chờ phản hồi.";
            default -> "Trạng thái yêu cầu của bạn đã được cập nhật.";
        };
    }

    /**
     * Get order request by ID
     * Only CUSTOMER, ADMIN, CUSTOMER_SUPPORT can access
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<OrderRequestDTO>> getOrderRequestById(@PathVariable UUID id) {
        
        OrderRequestDTO orderRequest = orderRequestService.getOrderRequestById(id);
        
        ApiResponse<OrderRequestDTO> apiResponse = ResponseUtil.success(
            orderRequest,
            "Order request retrieved successfully"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Get paginated order requests by user ID and status
     * Only CUSTOMER, ADMIN, CUSTOMER_SUPPORT can access
     * 
     * @param userId User ID to filter requests
     * @param status Request status (DANG_XU_LY, HOAN_THANH)
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Page of order requests
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<OrderRequestDTO>>> getOrderRequestsByUserAndStatus(
            @PathVariable UUID userId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // Create pageable with sort by createdAt DESC (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<OrderRequestDTO> orderRequests = orderRequestService.getOrderRequestsByUserAndStatus(
            userId, 
            status, 
            pageable
        );
        
        String message = status != null 
            ? "Order requests retrieved successfully for status: " + status
            : "All order requests retrieved successfully";
        
        ApiResponse<Page<OrderRequestDTO>> apiResponse = ResponseUtil.success(
            orderRequests,
            message
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Get all order requests with filter and pagination
     * Only ADMIN, CUSTOMER_SUPPORT can access
     * 
     * @param filter Filter type (all, DANG_XU_LY, HOAN_THANH)
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Page of order requests
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<OrderRequestDTO>>> getAllOrderRequestsWithFilter(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<OrderRequestDTO> orderRequests = orderRequestService.getAllOrderRequestsWithFilter(
            filter, 
            page, 
            size
        );
        
        String message = filter.equalsIgnoreCase("all")
            ? "All order requests retrieved successfully"
            : "Order requests retrieved successfully for filter: " + filter;
        
        ApiResponse<Page<OrderRequestDTO>> apiResponse = ResponseUtil.success(
            orderRequests,
            message
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Search order requests by keyword with filter
     * Only ADMIN, CUSTOMER_SUPPORT can access
     * 
     * @param keyword Search keyword (searches in reason, email, phone, orderCode, requestId)
     * @param filter Filter type (all, DANG_XU_LY, HOAN_THANH)
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Page of order requests matching the search criteria
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Page<OrderRequestDTO>>> searchOrderRequests(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<OrderRequestDTO> orderRequests = orderRequestService.searchOrderRequests(
            keyword,
            filter,
            page,
            size
        );
        
        String message = String.format(
            "Search completed - Found %d results for keyword: '%s'",
            orderRequests.getTotalElements(),
            keyword
        );
        
        ApiResponse<Page<OrderRequestDTO>> apiResponse = ResponseUtil.success(
            orderRequests,
            message
        );
        
        return ResponseEntity.ok(apiResponse);
    }
}

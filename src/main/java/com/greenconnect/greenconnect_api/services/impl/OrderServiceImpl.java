package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dto.OrderRequestDTO;
import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRequest;
import com.greenconnect.greenconnect_api.dtos.request.OrderItemRequest;
import com.greenconnect.greenconnect_api.dtos.response.OrderItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.OrderResponse;
import com.greenconnect.greenconnect_api.dtos.response.OrderStatusHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.VnpayPaymentResponse;
import com.greenconnect.greenconnect_api.entities.Order;
import com.greenconnect.greenconnect_api.entities.OrderDetail;
import com.greenconnect.greenconnect_api.entities.OrderStatusHistory;
import com.greenconnect.greenconnect_api.entities.OrderVoucher;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductReview;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.Voucher;
import com.greenconnect.greenconnect_api.entities.VoucherUsage;
import com.greenconnect.greenconnect_api.enums.CreatedBy;
import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.PaymentMethod;
import com.greenconnect.greenconnect_api.enums.PaymentStatus;
import com.greenconnect.greenconnect_api.enums.RefundStatus;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import com.greenconnect.greenconnect_api.enums.RequestType;
import com.greenconnect.greenconnect_api.enums.VoucherType;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.OrderDetailRepository;
import com.greenconnect.greenconnect_api.repositories.OrderRepository;
import com.greenconnect.greenconnect_api.repositories.OrderStatusHistoryRepository;
import com.greenconnect.greenconnect_api.repositories.OrderVoucherRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.ProductReviewRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.repositories.VoucherRepository;
import com.greenconnect.greenconnect_api.repositories.VoucherUsageRepository;
import com.greenconnect.greenconnect_api.services.CartService;
import com.greenconnect.greenconnect_api.services.OrderRequestService;
import com.greenconnect.greenconnect_api.services.OrderService;
import com.greenconnect.greenconnect_api.services.VnpayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository; // ⚡ Thêm để cập nhật sellNumber
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final OrderVoucherRepository orderVoucherRepository;
    private final ProductReviewRepository productReviewRepository;
    private final VnpayService vnpayService;
    private final com.greenconnect.greenconnect_api.services.EmailService emailService;
    private final OrderRequestService orderRequestService;
    private final com.greenconnect.greenconnect_api.services.NotificationDatabaseService notificationDatabaseService;
    private final com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService elasticsearchSyncService; // ⚡ Sync sellNumber to ES
    private final CartService cartService; // 🛒 Xóa sản phẩm đã đặt khỏi giỏ hàng

    @Override
    @Transactional
    public OrderResponse createCashOrder(UUID userId, CreateOrderRequest request) {
        log.info("Creating cash order for user: {}", userId);
        
        // Validation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // ⭐ OPTIMIZED: Lấy tất cả variants với Product và Images trong 1 query duy nhất
        List<UUID> variantIds = request.getOrderItems().stream()
                .map(OrderItemRequest::getProductVariantId)
                .collect(Collectors.toList());
        
        List<ProductVariant> variants = productVariantRepository
                .findByIdInWithProductAndImages(variantIds);
        
        // Map variant by ID để tra cứu nhanh O(1)
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        
        // Validate and process order items - Frontend handles all calculations
        List<OrderDetail> orderDetails = new ArrayList<>();
        
        for (OrderItemRequest item : request.getOrderItems()) {
            ProductVariant variant = variantMap.get(item.getProductVariantId());
            
            if (variant == null) {
                throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
            }
            
            // Check if product is active
            if (variant.getProduct() != null && !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
                log.error("Product is not active for variant {}: productId={}, productName={}", 
                    item.getProductVariantId(), variant.getProduct().getId(), variant.getProduct().getName());
                throw new AppException(ErrorCode.PRODUCT_NOT_SELLING);
            }
            
            // Check stock availability
            if (variant.getStockQuantity() < item.getQuantity()) {
                log.error("Insufficient stock for variant {}: requested={}, available={}", 
                    item.getProductVariantId(), item.getQuantity(), variant.getStockQuantity());
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            
            // Tạo OrderDetail với tên đầy đủ (Product name + Variant name)
            // Images đã được eager load từ Product
            OrderDetail orderDetail = OrderDetail.builder()
                    .quantity(item.getQuantity())
                    .variant(variant)
                    .productName(variant.getFullName()) // Product name + " - " + Variant name
                    .productImageUrl(variant.getMainImageUrl()) // Lấy ảnh chính từ Product.mainImageUrl
                    .unit(variant.getUnit())
                    .originalPricePerUnit(variant.getPrice())
                    .sellingPricePerUnit(variant.getDiscountedPrice())
                    .discountPercentage(variant.getDiscountPercentage())
                    .build();
            
            orderDetails.add(orderDetail);
        }
        
        // ⭐ Nếu totalPayment = 0, tự động set paymentStatus = DA_THANH_TOAN
        PaymentStatus initialPaymentStatus = request.getTotalPayment().compareTo(BigDecimal.ZERO) == 0 
                ? PaymentStatus.DA_THANH_TOAN 
                : PaymentStatus.CHUA_THANH_TOAN;
        
        if (initialPaymentStatus == PaymentStatus.DA_THANH_TOAN) {
            log.info("💰 [COD] totalPayment = 0, tự động set paymentStatus = DA_THANH_TOAN");
        }
        
        // Create Order
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .totalProductAmount(request.getTotalProductMoney())
                .shippingFee(request.getShippingFee())
                .discountAmount(request.getDiscountAmount())
                .tax(request.getTax())
                .totalPayment(request.getTotalPayment())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(initialPaymentStatus)
                .orderStatus(OrderStatus.DANG_CHO)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .deliveryAddress(request.getDeliveryAddress())
               // .deliveryAddressSnapshot(request.getDeliveryAddress()) // Snapshot tại thời điểm đặt hàng
                .deliveryDate(request.getDeliveryDate())
                .customerNote(request.getCustomerNote())
                .loyaltyPoints(request.getLoyaltyPoints())
                .rankPoints(request.getRankPoints())
                .orderDate(getNowVietnam()) // ✅ Set orderDate với timezone Việt Nam
                .build();

        order = orderRepository.save(order);
        
        // ⭐ DEDUCT LOYALTY POINTS FOR COD ORDERS IMMEDIATELY
        // For VNPay orders, points will be deducted when payment status changes to DA_THANH_TOAN
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            if (request.getLoyaltyPoints() != null && request.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                BigDecimal usedPoints = request.getLoyaltyPoints();
                
                // ⚠️ Validate: Check if loyaltyPoints is mistakenly set to totalPayment
                if (usedPoints.compareTo(request.getTotalPayment()) == 0 && usedPoints.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    log.error("❌ Frontend error detected: loyaltyPoints ({}) = totalPayment ({}). Frontend đang gửi sai dữ liệu!", 
                        usedPoints, request.getTotalPayment());
                    throw new AppException(ErrorCode.INVALID_REQUEST, 
                        "Số điểm loyalty không hợp lệ. Frontend đang gửi totalPayment thay vì loyaltyPoints!");
                }
                
                // Validate user has enough points
                if (currentLoyaltyPoints.compareTo(usedPoints) < 0) {
                    throw new AppException(ErrorCode.INSUFFICIENT_LOYALTY_POINTS, 
                        String.format("User chỉ có %.2f điểm nhưng cố dùng %.2f điểm. Vui lòng kiểm tra số dư tài khoản!", 
                            currentLoyaltyPoints, usedPoints));
                }
                
                // Deduct points from user
                user.setLoyaltyPoints(currentLoyaltyPoints.subtract(usedPoints));
                userRepository.save(user);
                
                log.info("🔻 [COD] Đã trừ {} loyalty points từ user {}. Cũ: {}, Mới: {}", 
                    usedPoints, userId, currentLoyaltyPoints, user.getLoyaltyPoints());
            }
        } else {
            log.info("💳 [VNPAY] Chưa trừ loyalty points - sẽ trừ khi thanh toán thành công");
        }

        // Process vouchers if provided (Many-to-Many)
        if (request.getVoucherIds() != null && !request.getVoucherIds().isEmpty()) {
            validateAndProcessVouchers(request.getVoucherIds(), userId, order);
        }

        // Save order details
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(order);
        }
        orderDetailRepository.saveAll(orderDetails);

        // Create status history
        String historyNote = request.getPaymentMethod() == PaymentMethod.COD 
            ? "Đơn hàng được tạo thành công - COD" 
            : "Đơn hàng VNPay được tạo - chờ thanh toán";
        createOrderStatusHistory(order, OrderStatus.DANG_CHO, historyNote);

        // ⭐ ONLY DEDUCT STOCK FOR COD ORDERS
        // VNPay orders will deduct stock after successful payment
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            for (OrderItemRequest item : request.getOrderItems()) {
                ProductVariant variant = variantMap.get(item.getProductVariantId());
                variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            }
            productVariantRepository.saveAll(variants);
            log.info("Stock deducted for COD order: {}", order.getOrderCode());
        } else {
            log.info("Stock NOT deducted yet for VNPay order: {} - waiting for payment", order.getOrderCode());
        }

        // Set relationships for response
        order.setOrderDetails(orderDetails);

        log.info("Cash order created successfully with ID: {}", order.getId());
        
        // 🛒 Xóa các sản phẩm đã đặt hàng ra khỏi giỏ hàng
        removeOrderedItemsFromCart(order.getId(), userId);
        
        // Send order confirmation email
        sendOrderConfirmationEmail(user, order, orderDetails);
        
        // 📢 Tạo thông báo đơn hàng mới cho tất cả admin/manager
        try {
            notificationDatabaseService.createNotificationForMultipleUsers(
                Arrays.asList(user.getId()), // userId của customer (chủ sở hữu notification)
                "don_hang",
                "Đơn hàng mới " + order.getOrderCode() + " cần xử lý",
                "Khách hàng " + user.getFullName() + " (" + user.getUserCode() + ") vừa tạo đơn hàng COD. Tổng tiền: " + 
                    String.format("%,.0f", order.getTotalPayment()) + " VNĐ. Vui lòng xác nhận đơn hàng.",
                null,
                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
            );
            log.info("✅ Đã tạo thông báo đơn hàng mới COD cho tất cả managers: {}", order.getOrderCode());
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo thông báo: {}", e.getMessage());
        }
        
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public VnpayPaymentResponse createVnpayOrder(UUID userId, CreateOrderRequest request, String clientIp) {
        log.info("Creating VNPay order for user: {}", userId);
        
        // ⭐ CRITICAL FIX: VNPay does NOT accept decimal amounts (e.g., 807957.5 VND)
        // Round all financial values to nearest integer to avoid Error 71
        request.setTotalProductMoney(request.getTotalProductMoney().setScale(0, java.math.RoundingMode.HALF_UP));
        request.setShippingFee(request.getShippingFee().setScale(0, java.math.RoundingMode.HALF_UP));
        request.setDiscountAmount(request.getDiscountAmount().setScale(0, java.math.RoundingMode.HALF_UP));
        if (request.getTax() != null) {
            request.setTax(request.getTax().setScale(0, java.math.RoundingMode.HALF_UP));
        }
        request.setTotalPayment(request.getTotalPayment().setScale(0, java.math.RoundingMode.HALF_UP));
        
        log.info("💳 [VNPAY] Rounded totalPayment: {} (original might have decimals)", request.getTotalPayment());
        
        // Validation
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // ⭐ OPTIMIZED: Lấy tất cả variants với Product và Images trong 1 query duy nhất
        List<UUID> variantIds = request.getOrderItems().stream()
                .map(OrderItemRequest::getProductVariantId)
                .collect(Collectors.toList());
        
        List<ProductVariant> variants = productVariantRepository
                .findByIdInWithProductAndImages(variantIds);
        
        // Map variant by ID để tra cứu nhanh O(1)
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        
        // Validate and process order items - Frontend handles all calculations
        List<OrderDetail> orderDetails = new ArrayList<>();
        
        for (OrderItemRequest item : request.getOrderItems()) {
            ProductVariant variant = variantMap.get(item.getProductVariantId());
            
            if (variant == null) {
                throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
            }
            
            // Check if product is active
            if (variant.getProduct() != null && !Boolean.TRUE.equals(variant.getProduct().getIsActive())) {
                log.error("Product is not active for variant {}: productId={}, productName={}", 
                    item.getProductVariantId(), variant.getProduct().getId(), variant.getProduct().getName());
                throw new AppException(ErrorCode.PRODUCT_NOT_SELLING);
            }
            
            // Check stock availability
            if (variant.getStockQuantity() < item.getQuantity()) {
                log.error("Insufficient stock for variant {}: requested={}, available={}", 
                    item.getProductVariantId(), item.getQuantity(), variant.getStockQuantity());
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            
            // Tạo OrderDetail với tên đầy đủ (Product name + Variant name)
            OrderDetail orderDetail = OrderDetail.builder()
                    .quantity(item.getQuantity())
                    .variant(variant)
                    .productName(variant.getFullName())
                    .productImageUrl(variant.getMainImageUrl())
                    .unit(variant.getUnit())
                    .originalPricePerUnit(variant.getPrice())
                    .sellingPricePerUnit(variant.getDiscountedPrice())
                    .discountPercentage(variant.getDiscountPercentage())
                    .build();
            
            orderDetails.add(orderDetail);
        }
        
        // ⭐ Nếu totalPayment = 0, tự động set paymentStatus = DA_THANH_TOAN
        PaymentStatus initialPaymentStatus = request.getTotalPayment().compareTo(BigDecimal.ZERO) == 0 
                ? PaymentStatus.DA_THANH_TOAN 
                : PaymentStatus.CHUA_THANH_TOAN;
        
        if (initialPaymentStatus == PaymentStatus.DA_THANH_TOAN) {
            log.info("💰 [VNPAY] totalPayment = 0, tự động set paymentStatus = DA_THANH_TOAN");
        }
        
        // Create Order with VNPAY payment method
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .totalProductAmount(request.getTotalProductMoney())
                .shippingFee(request.getShippingFee())
                .discountAmount(request.getDiscountAmount())
                .tax(request.getTax())
                .totalPayment(request.getTotalPayment())
                .paymentMethod(PaymentMethod.VNPAY) // ⭐ VNPAY from start
                .paymentStatus(initialPaymentStatus)
                .orderStatus(OrderStatus.DANG_CHO)
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryDate(request.getDeliveryDate())
                .customerNote(request.getCustomerNote())
                .loyaltyPoints(request.getLoyaltyPoints())
                .rankPoints(request.getRankPoints())
                .orderDate(getNowVietnam()) // ✅ Set orderDate với timezone Việt Nam
                .build();

        order = orderRepository.save(order);
        
        // ⭐ VNPAY: NO loyalty points deduction, NO stock deduction (wait for payment)
        log.info("💳 [VNPAY] Order created - NO loyalty/stock deduction until payment confirmed");

        // Process vouchers if provided (Many-to-Many)
        if (request.getVoucherIds() != null && !request.getVoucherIds().isEmpty()) {
            validateAndProcessVouchers(request.getVoucherIds(), userId, order);
        }

        // Save order details
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(order);
        }
        orderDetailRepository.saveAll(orderDetails);

        // Create status history
        createOrderStatusHistory(order, OrderStatus.DANG_CHO, "Đơn hàng VNPay được tạo - chờ thanh toán");

        // Set relationships for response
        order.setOrderDetails(orderDetails);
        
        // Generate proper VNPay payment URL
        String orderInfo = String.format("Thanh toan don hang #%s", order.getOrderCode());
        String paymentUrl = vnpayService.createPaymentUrl(
                order.getOrderCode(),
                order.getTotalPayment(),
                orderInfo,
                clientIp
        );
        
        log.info("VNPay payment URL generated for order: {}", order.getOrderCode());
        
        // Send order confirmation email
        sendOrderConfirmationEmail(user, order, orderDetails);
        
        return VnpayPaymentResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .paymentUrl(paymentUrl)
                .totalAmount(order.getTotalPayment())
                .message("VNPay payment URL created successfully")
                .build();
    }

    @Override
    @Transactional
    public OrderResponse handleVnpayReturn(Map<String, String> params) {
        log.info("Processing VNPay return with params: {}", params.keySet());
        
        // 1. Validate secure hash
        if (!vnpayService.validateSecureHash(params)) {
            log.error("Invalid VNPay secure hash");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // 2. Parse payment result
        VnpayService.VnpayPaymentResult paymentResult = vnpayService.parsePaymentResult(params);
        
        Order order = orderRepository.findByOrderCode(paymentResult.orderCode())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
                
        if (paymentResult.isSuccess()) {
            // Payment successful - use updatePaymentStatus for full business logic
            String historyNote = String.format("Thanh toán VNPay thành công - Mã GD: %s, Ngân hàng: %s", 
                    paymentResult.transactionNo(), paymentResult.bankCode());
            
            // ⭐ LƯU vnpayTransactionNo để dùng cho refund sau này
            order.setVnpayTransactionNo(paymentResult.transactionNo());
            orderRepository.save(order);
            
            // ⭐ Call updatePaymentStatus to handle loyalty points + total payment + stock deduction
            updatePaymentStatus(order.getId(), PaymentStatus.DA_THANH_TOAN.name(), historyNote);
            
            log.info("VNPay payment successful for order: {}, transaction: {} - SAVED vnpayTransactionNo", 
                    paymentResult.orderCode(), paymentResult.transactionNo());
            
        } else {
            // Payment failed
            order.setPaymentStatus(PaymentStatus.THAT_BAI);
            order.setOrderStatus(OrderStatus.DA_HUY);
            
            // Rollback all vouchers if used (Many-to-Many)
            List<OrderVoucher> orderVouchers = orderVoucherRepository.findByOrderIdWithVoucher(order.getId());
            for (OrderVoucher ov : orderVouchers) {
                rollbackVoucher(ov.getVoucher().getId(), order.getUser().getId(), order.getId());
            }
            
            // Create status history
            String message = String.format("Thanh toán VNPay thất bại - Mã lỗi: %s", paymentResult.responseCode());
            createOrderStatusHistory(order, OrderStatus.DA_HUY, message);
            
            // ⭐ NO NEED TO ROLLBACK STOCK - never deducted!
            log.info("No stock rollback needed for order: {} - stock was never deducted", paymentResult.orderCode());
            
            log.warn("VNPay payment failed for order: {}, response code: {}", 
                    paymentResult.orderCode(), paymentResult.responseCode());
                    
            orderRepository.save(order);
        }
        
        // Reload order to get updated data
        order = orderRepository.findById(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Send email notification for successful VNPay payment
        if (paymentResult.isSuccess()) {
            sendPaymentSuccessEmail(order);
            
            // 📢 Tạo thông báo đơn hàng VNPay thanh toán thành công cho tất cả admin/manager
            try {
                notificationDatabaseService.createNotificationForMultipleUsers(
                    Arrays.asList(order.getUser().getId()), // userId của customer (chủ sở hữu notification)
                    "don_hang",
                    "Đơn hàng VNPay " + order.getOrderCode() + " đã thanh toán thành công",
                    "Khách hàng " + order.getUser().getFullName() + " (" + order.getUser().getUserCode() + ") đã thanh toán VNPay thành công. Tổng tiền: " + 
                        String.format("%,.0f", order.getTotalPayment()) + " VNĐ. Vui lòng xác nhận đơn hàng.",
                    null,
                    com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                );
                log.info("✅ Đã tạo thông báo đơn hàng VNPay thanh toán thành công cho tất cả managers: {}", order.getOrderCode());
            } catch (Exception e) {
                log.error("❌ Lỗi khi tạo thông báo VNPay: {}", e.getMessage());
            }
        }
        
        return mapToOrderResponse(order);
    }

    @Override
    public boolean handleVnpayCallback(Map<String, String> params) {
        try {
            log.info("Processing VNPay IPN callback for order: {}", params.get("vnp_TxnRef"));
            handleVnpayReturn(params);
            return true;
        } catch (Exception e) {
            log.error("VNPay callback failed", e);
            return false;
        }
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderResponse(order);
    }

    @Override
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
        return orders.map(this::mapToOrderResponse);
    }

    @Override
    public OrderResponse checkOrderPaymentStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderResponse(order);
    }
    
    @Override
    public OrderResponse checkOrderPaymentStatusByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> items = new ArrayList<>();
        
        if (order.getOrderDetails() != null) {
            items = order.getOrderDetails().stream()
                    .map(detail -> {
                        BigDecimal totalPrice = detail.getSellingPricePerUnit().multiply(BigDecimal.valueOf(detail.getQuantity()));
                        BigDecimal totalOriginalPrice = detail.getOriginalPricePerUnit().multiply(BigDecimal.valueOf(detail.getQuantity()));
                        BigDecimal totalDiscount = totalOriginalPrice.subtract(totalPrice);
                        
                        // ⭐ Kiểm tra orderDetail này đã có review chưa (theo orderDetailId)
                        boolean hasReviewed = productReviewRepository.existsByOrderDetailId(detail.getId());
                        
                        // ⭐ CHỈ lấy review CỤ THỂ của OrderDetail này (không lấy tất cả reviews của product)
                        List<ProductReviewResponse> reviewList = new ArrayList<>();
                        UUID productId = null;
                        
                        if (detail.getVariant() != null && detail.getVariant().getProduct() != null) {
                            productId = detail.getVariant().getProduct().getId();
                            
                            // 🔥 QUAN TRỌNG: Chỉ lấy review của OrderDetail này, KHÔNG phải tất cả reviews của product
                            java.util.Optional<ProductReview> reviewOpt = productReviewRepository.findByOrderDetailIdWithDetails(detail.getId());
                            
                            if (reviewOpt.isPresent()) {
                                ProductReview review = reviewOpt.get();
                                ProductReviewResponse reviewResponse = ProductReviewResponse.builder()
                                        .id(review.getId())
                                        .rating(review.getRating())
                                        .comment(review.getComment() != null ? review.getComment() : "")
                                        .reviewTime(review.getReviewTime())
                                        .user(ProductReviewResponse.UserInfo.builder()
                                                .userId(review.getUser().getId())
                                                .fullName(review.getUser().getFullName() != null ? review.getUser().getFullName() : "")
                                                .avatarUrl(review.getUser().getAvatarUrl() != null ? review.getUser().getAvatarUrl() : "")
                                                .build())
                                        .mediaList(review.getReviewMedia().stream()
                                                .map(media -> ProductReviewResponse.MediaInfo.builder()
                                                        .mediaType(media.getMediaType() != null ? media.getMediaType().name() : "IMAGE")
                                                        .mediaUrl(media.getMediaUrl() != null ? media.getMediaUrl() : "")
                                                        .build())
                                                .toList())
                                        .shopReply(review.getShopReply() != null ? 
                                                ProductReviewResponse.ShopReplyInfo.builder()
                                                        .content(review.getShopReply())
                                                        .repliedAt(review.getShopReplyAt())
                                                        .replierName(review.getShopReplier() != null ? 
                                                                (review.getShopReplier().getFullName() != null ? review.getShopReplier().getFullName() : "") : "")
                                                        .build() : null)
                                        .build();
                                
                                reviewList.add(reviewResponse);
                            }
                        }
                        
                        return OrderItemResponse.builder()
                                .id(detail.getId())
                                .productVariantId(detail.getVariant().getId())
                                .productId(productId)
                                .productName(detail.getProductName())
                                .productImageUrl(detail.getProductImageUrl())
                                .unit(detail.getUnit())
                                .quantity(detail.getQuantity())
                                .originalPricePerUnit(detail.getOriginalPricePerUnit())
                                .sellingPricePerUnit(detail.getSellingPricePerUnit())
                                .discountPercentage(detail.getDiscountPercentage())
                                .totalPrice(totalPrice)
                                .totalOriginalPrice(totalOriginalPrice)
                                .totalDiscount(totalDiscount)
                                .reviews(reviewList)  // 🔥 List chỉ chứa 0 hoặc 1 review của OrderDetail này
                                .hasReviewed(hasReviewed)
                                .build();
                    })
                    .toList();
        }
        
        // Load vouchers from junction table (Many-to-Many)
        List<OrderVoucher> orderVouchers = orderVoucherRepository.findByOrderIdWithVoucher(order.getId());
        List<OrderResponse.VoucherInfo> voucherInfos = orderVouchers.stream()
                .map(ov -> OrderResponse.VoucherInfo.builder()
                        .voucherId(ov.getVoucher().getId())
                        .voucherCode(ov.getVoucher().getVoucherCode())
                        .discountApplied(ov.getDiscountApplied())
                        .appliedAt(ov.getAppliedAt())
                        .build())
                .toList();

        // ⭐ Load order status history
        List<OrderStatusHistoryResponse> statusHistory = getOrderStatusHistory(order.getId());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .userName(order.getUser().getFullName())
                .vouchers(voucherInfos)
                .totalProductAmount(order.getTotalProductAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .tax(order.getTax())
                .totalPayment(order.getTotalPayment())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                //.deliveryAddressSnapshot(order.getDeliveryAddressSnapshot())
                .deliveryDate(order.getDeliveryDate())
                .customerNote(order.getCustomerNote())
                .loyaltyPoints(order.getLoyaltyPoints())
                .rankPoints(order.getRankPoints())
                .orderDate(order.getOrderDate())
                .actualDeliveryDate(order.getActualDeliveryDate())
                .orderItems(items)
                .statusHistory(statusHistory)
                .build();
    }

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis();
    }
    
    /**
     * Helper method để tạo lịch sử trạng thái đơn hàng
     */
    private void createOrderStatusHistory(Order order, OrderStatus status, String note) {
        OrderStatusHistory statusHistory = OrderStatusHistory.builder()
                .order(order)
                .status(status.name())
                .note(note)
                .build();
        orderStatusHistoryRepository.save(statusHistory);
        log.info("Created status history for order {}: {} - {}", order.getOrderCode(), status.name(), note);
    }

    /**
     * Validate và xử lý voucher khi tạo đơn hàng
     */
    @Transactional
    /**
     * Validate and process multiple vouchers (Many-to-Many)
     */
    private void validateAndProcessVouchers(List<UUID> voucherIds, UUID userId, Order order) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return;
        }

        for (UUID voucherId : voucherIds) {
            // Tìm voucher
            Voucher voucher = voucherRepository.findById(voucherId)
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            // Kiểm tra voucher có còn hoạt động không
            if (!voucher.getIsActive()) {
                throw new AppException(ErrorCode.VOUCHER_INACTIVE);
            }

            // Kiểm tra thời gian hiệu lực
            java.time.LocalDateTime now = getNowVietnam();
            if (voucher.getStartDate().isAfter(now) || voucher.getEndDate().isBefore(now)) {
                throw new AppException(ErrorCode.VOUCHER_EXPIRED);
            }

            // Kiểm tra giá trị đơn hàng tối thiểu
            if (voucher.getMinOrderValue() != null && order.getTotalProductAmount().compareTo(voucher.getMinOrderValue()) < 0) {
                throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_VALUE_NOT_MET);
            }

            // Kiểm tra số lượng sử dụng tối đa
            if (voucher.getMaxUsageCount() != null && voucher.getUsedCount() >= voucher.getMaxUsageCount()) {
                throw new AppException(ErrorCode.VOUCHER_USAGE_LIMIT_EXCEEDED);
            }

            // Kiểm tra user đã sử dụng voucher này chưa (nếu voucher chỉ được dùng 1 lần/user)
            long userUsageCount = voucherUsageRepository.countByVoucherIdAndUserId(voucherId, userId);
            if (voucher.getUsageLimitPerUser() != null && userUsageCount >= voucher.getUsageLimitPerUser()) {
                throw new AppException(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED);
            }

            // Kiểm tra duplicate (order không được dùng voucher trùng)
            if (orderVoucherRepository.existsByOrderIdAndVoucherId(order.getId(), voucherId)) {
                throw new AppException(ErrorCode.VOUCHER_ALREADY_USED_IN_ORDER);
            }

            // Calculate discount for this voucher (can be percentage or fixed amount)
            BigDecimal discountApplied = calculateVoucherDiscount(voucher, order);

            // Save to junction table
            OrderVoucher orderVoucher = OrderVoucher.builder()
                    .order(order)
                    .voucher(voucher)
                    .discountApplied(discountApplied)
                    .build();
            orderVoucherRepository.save(orderVoucher);

            // Cập nhật số lượng đã sử dụng của voucher
            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);

            // Tạo lịch sử sử dụng voucher
            VoucherUsage voucherUsage = VoucherUsage.builder()
                    .user(userRepository.findById(userId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)))
                    .voucher(voucher)
                    .order(order)
                    .build();
            voucherUsageRepository.save(voucherUsage);

            log.info("Voucher {} processed successfully for user {} in order {}", 
                    voucherId, userId, order.getOrderCode());
        }
    }

    /**
     * Calculate discount amount for a voucher
     */
    private BigDecimal calculateVoucherDiscount(Voucher voucher, Order order) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        if (voucher.getVoucherType() == VoucherType.PERCENTAGE) {
            // Percentage discount
            discountAmount = order.getTotalProductAmount()
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            // Apply max discount if exists
            if (voucher.getMaxDiscountAmount() != null && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        } else {
            // Fixed amount discount
            discountAmount = voucher.getDiscountValue();
        }
        
        return discountAmount;
    }



    /**
     * Rollback voucher khi có lỗi xảy ra trong quá trình tạo đơn hàng
     */
    @Transactional
    private void rollbackVoucher(UUID voucherId, UUID userId, UUID orderId) {
        if (voucherId == null) {
            return;
        }

        try {
            // Tìm và xóa VoucherUsage
            List<VoucherUsage> usages = voucherUsageRepository.findByVoucherIdAndUserId(voucherId, userId);
            for (VoucherUsage usage : usages) {
                if (usage.getOrder().getId().equals(orderId)) {
                    voucherUsageRepository.delete(usage);
                    log.info("Deleted voucher usage for voucher {} and order {}", voucherId, orderId);
                    break;
                }
            }

            // Giảm used count của voucher
            Voucher voucher = voucherRepository.findById(voucherId).orElse(null);
            if (voucher != null && voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
                log.info("Rolled back voucher usage count for voucher {}", voucherId);
            }
        } catch (Exception e) {
            log.error("Error rolling back voucher {}: {}", voucherId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String newStatusStr, String note) {
        log.info("🔄 Admin cập nhật trạng thái đơn hàng - OrderId: {}, NewStatus: {}", orderId, newStatusStr);
        
        // ⭐ Delegate to updateOrderStatusWithCashback to ensure full business logic
        // This includes: loyalty points, totalPaymentAmount, cashback, stock, voucher handling
        return updateOrderStatusWithCashback(orderId, newStatusStr, note);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(UUID orderId) {
        // Kiểm tra đơn hàng có tồn tại không
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        
        // Lấy lịch sử trạng thái theo thứ tự thời gian
        List<OrderStatusHistory> histories = orderStatusHistoryRepository
                .findByOrderIdOrderByUpdatedTimeAsc(orderId);
        
        return histories.stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .orderId(history.getOrder().getId())
                        .status(history.getStatus())
                        .note(history.getNote())
                        .updatedTime(history.getUpdatedTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateCodPaymentStatus(UUID orderId, boolean isPaid, String note) {
        // Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Kiểm tra đây có phải đơn hàng COD không
        if (order.getPaymentMethod() != PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ có thể cập nhật trạng thái thanh toán cho đơn hàng COD");
        }
        
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        PaymentStatus newPaymentStatus = isPaid ? PaymentStatus.DA_THANH_TOAN : PaymentStatus.CHUA_THANH_TOAN;
        
        // Cập nhật trạng thái thanh toán
        order.setPaymentStatus(newPaymentStatus);
        
        // Lưu đơn hàng
        order = orderRepository.save(order);
        
        // Tạo OrderStatusHistory cho việc thanh toán
        String historyNote = note != null ? note : 
                (isPaid ? "Đã nhận thanh toán COD từ khách hàng" : "Chưa nhận thanh toán COD từ khách hàng");
        
        createOrderStatusHistory(order, order.getOrderStatus(), 
                String.format("Cập nhật thanh toán: %s → %s. %s", 
                        oldPaymentStatus, newPaymentStatus, historyNote));
        
        log.info("Updated COD payment status for order {} from {} to {}", 
                orderId, oldPaymentStatus, newPaymentStatus);
        
        return mapToOrderResponse(order);
    }
    
    // ==================== CART MANAGEMENT METHODS ====================
    
    /**
     * 🛒 Xóa các sản phẩm đã đặt hàng thành công ra khỏi giỏ hàng
     * 
     * <p><b>Logic:</b></p>
     * <ul>
     *   <li>Lấy OrderDetail từ orderId để biết sản phẩm nào + số lượng bao nhiêu</li>
     *   <li>Duyệt qua từng OrderDetail và gọi CartService để xóa từng sản phẩm</li>
     *   <li>Xử lý lỗi an toàn - không làm fail transaction đơn hàng nếu cart có vấn đề</li>
     * </ul>
     * 
     * <p><b>Được gọi ở:</b></p>
     * <ul>
     *   <li>createCashOrder() - Sau khi tạo đơn COD thành công</li>
     *   <li>updatePaymentStatus() - Khi VNPay thanh toán thành công (DA_THANH_TOAN)</li>
     * </ul>
     * 
     * @param orderId ID của đơn hàng đã tạo thành công
     * @param userId ID của user để xác định giỏ hàng cần xóa
     */
    private void removeOrderedItemsFromCart(UUID orderId, UUID userId) {
        try {
            log.info("🛒 Bắt đầu xóa sản phẩm đã đặt hàng khỏi giỏ hàng - OrderId: {}, UserId: {}", orderId, userId);
            
            // 1. Lấy Order để lấy OrderDetails
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            
            List<OrderDetail> orderDetails = order.getOrderDetails();
            
            if (orderDetails == null || orderDetails.isEmpty()) {
                log.warn("⚠️ Đơn hàng {} không có OrderDetail nào", orderId);
                return;
            }
            
            // 2. Duyệt qua từng OrderDetail và xóa khỏi giỏ hàng
            int removedCount = 0;
            for (OrderDetail detail : orderDetails) {
                try {
                    UUID productVariantId = detail.getVariant().getId();
                    
                    // Kiểm tra xem sản phẩm có trong giỏ hàng không
                    if (cartService.isItemInCart(userId, productVariantId)) {
                        // Lấy CartItem để lấy cartItemId
                        var cartItemOpt = cartService.getCartItemByUserAndVariant(userId, productVariantId);
                        
                        if (cartItemOpt.isPresent()) {
                            UUID cartItemId = cartItemOpt.get().getId();
                            cartService.removeCartItem(userId, cartItemId);
                            removedCount++;
                            log.info("✅ Đã xóa sản phẩm {} khỏi giỏ hàng - CartItemId: {}", 
                                    detail.getProductName(), cartItemId);
                        }
                    } else {
                        log.debug("ℹ️ Sản phẩm {} không có trong giỏ hàng - Bỏ qua", detail.getProductName());
                    }
                    
                } catch (Exception e) {
                    log.warn("⚠️ Lỗi khi xóa sản phẩm {} khỏi giỏ hàng: {}", 
                            detail.getProductName(), e.getMessage());
                    // Continue với sản phẩm tiếp theo - không fail toàn bộ transaction
                }
            }
            
            log.info("✅ Hoàn thành xóa giỏ hàng - Đã xóa {}/{} sản phẩm", removedCount, orderDetails.size());
            
        } catch (Exception e) {
            // ⚠️ Log error nhưng KHÔNG throw exception - tránh fail transaction đơn hàng
            log.error("❌ Lỗi khi xóa sản phẩm đã đặt hàng khỏi giỏ hàng - OrderId: {}, UserId: {}, Error: {}", 
                    orderId, userId, e.getMessage());
        }
    }
    
    // ==================== EMAIL NOTIFICATION METHODS ====================
    
    /**
     * Gửi email xác nhận đơn hàng sau khi đặt hàng thành công
     */
    private void sendOrderConfirmationEmail(User user, Order order, List<OrderDetail> orderDetails) {
        try {
            String subject = String.format("✅ Xác nhận đơn hàng #%s - GreenConnect", order.getOrderCode());
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append(String.format("Kính chào %s,\n\n", user.getFullName()));
            emailBody.append(String.format("Cảm ơn bạn đã đặt hàng tại GreenConnect!\n\n"));
            emailBody.append(String.format("🛒 THÔNG TIN ĐỌN HÀNG\n"));
            emailBody.append(String.format("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"));
            emailBody.append(String.format("Mã đơn hàng: %s\n", order.getOrderCode()));
            emailBody.append(String.format("Ngày đặt: %s\n", order.getOrderDate()));
            emailBody.append(String.format("Trạng thái: %s\n", getOrderStatusText(order.getOrderStatus())));
            emailBody.append(String.format("Phương thức thanh toán: %s\n\n", getPaymentMethodText(order.getPaymentMethod())));
            
            emailBody.append("📦 CHI TIẾT SẢN PHẨM\n");
            emailBody.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            for (OrderDetail detail : orderDetails) {
                emailBody.append(String.format("• %s\n", detail.getProductName()));
                emailBody.append(String.format("  Số lượng: %d x %,.0f đ\n", 
                    detail.getQuantity(), 
                    detail.getSellingPricePerUnit()));
                emailBody.append(String.format("  Thành tiền: %,.0f đ\n\n", 
                    detail.getSellingPricePerUnit().multiply(BigDecimal.valueOf(detail.getQuantity()))));
            }
            
            emailBody.append("💰 TỔNG TIỀN\n");
            emailBody.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            emailBody.append(String.format("Tổng tiền hàng: %,.0f đ\n", order.getTotalProductAmount()));
            emailBody.append(String.format("Phí vận chuyển: %,.0f đ\n", order.getShippingFee()));
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                emailBody.append(String.format("Giảm giá: -%,.0f đ\n", order.getDiscountAmount()));
            }
            if (order.getTax() != null && order.getTax().compareTo(BigDecimal.ZERO) > 0) {
                emailBody.append(String.format("Thuế: %,.0f đ\n", order.getTax()));
            }
            emailBody.append(String.format("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"));
            emailBody.append(String.format("TỔNG CỘNG: %,.0f đ\n\n", order.getTotalPayment()));
            
            emailBody.append("📍 THÔNG TIN GIAO HÀNG\n");
            emailBody.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            emailBody.append(String.format("Người nhận: %s\n", order.getRecipientName()));
            emailBody.append(String.format("Số điện thoại: %s\n", order.getRecipientPhone()));
            emailBody.append(String.format("Địa chỉ: %s\n", order.getDeliveryAddress()));
            if (order.getDeliveryDate() != null) {
                emailBody.append(String.format("Ngày giao dự kiến: %s\n", order.getDeliveryDate()));
            }
            if (order.getCustomerNote() != null && !order.getCustomerNote().isEmpty()) {
                emailBody.append(String.format("Ghi chú: %s\n", order.getCustomerNote()));
            }
            emailBody.append("\n");
            
            emailBody.append("Bạn có thể theo dõi đơn hàng của mình trong mục \"Đơn hàng của tôi\" trên ứng dụng GreenConnect.\n\n");
            emailBody.append("Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua:\n");
            emailBody.append("📧 Email: support@greenconnect.vn\n");
            emailBody.append("📞 Hotline: 1900-xxxx\n\n");
            emailBody.append("Trân trọng,\n");
            emailBody.append("Đội ngũ GreenConnect");
            
            // Send email asynchronously
            emailService.sendPlainEmailAsync(user.getEmail(), subject, emailBody.toString());
            log.info("Sent order confirmation email to: {} for order: {}", user.getEmail(), order.getOrderCode());
            
        } catch (Exception e) {
            // Don't fail the order creation if email fails
            log.error("Failed to send order confirmation email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
    
    /**
     * Gửi email thông báo thanh toán VNPay thành công
     */
    private void sendPaymentSuccessEmail(Order order) {
        try {
            User user = order.getUser();
            String subject = String.format("💳 Thanh toán thành công đơn hàng #%s - GreenConnect", order.getOrderCode());
            
            StringBuilder emailBody = new StringBuilder();
            emailBody.append(String.format("Kính chào %s,\n\n", user.getFullName()));
            emailBody.append("Thanh toán của bạn đã được xác nhận thành công! ✅\n\n");
            emailBody.append("🛒 THÔNG TIN ĐƠN HÀNG\n");
            emailBody.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            emailBody.append(String.format("Mã đơn hàng: %s\n", order.getOrderCode()));
            emailBody.append(String.format("Số tiền thanh toán: %,.0f đ\n", order.getTotalPayment()));
            emailBody.append(String.format("Phương thức: VNPay/VietQR\n"));
            emailBody.append(String.format("Trạng thái thanh toán: Đã thanh toán ✓\n"));
            emailBody.append(String.format("Trạng thái đơn hàng: %s\n\n", getOrderStatusText(order.getOrderStatus())));
            
            emailBody.append("📍 THÔNG TIN GIAO HÀNG\n");
            emailBody.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            emailBody.append(String.format("Người nhận: %s\n", order.getRecipientName()));
            emailBody.append(String.format("Số điện thoại: %s\n", order.getRecipientPhone()));
            emailBody.append(String.format("Địa chỉ: %s\n\n", order.getDeliveryAddress()));
            
            emailBody.append("Đơn hàng của bạn sẽ được xử lý và giao trong thời gian sớm nhất.\n");
            emailBody.append("Bạn có thể theo dõi trạng thái đơn hàng trong mục \"Đơn hàng của tôi\" trên ứng dụng.\n\n");
            emailBody.append("Cảm ơn bạn đã tin tướng và mua sắm tại GreenConnect! 🌿\n\n");
            emailBody.append("Trân trọng,\n");
            emailBody.append("Đội ngũ GreenConnect");
            
            emailService.sendPlainEmailAsync(user.getEmail(), subject, emailBody.toString());
            log.info("Sent payment success email to: {} for order: {}", user.getEmail(), order.getOrderCode());
            
        } catch (Exception e) {
            log.error("Failed to send payment success email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
    
    /**
     * Chuyển đổi OrderStatus enum sang text tiếng Việt
     */
        private String getOrderStatusText(OrderStatus status) {
            return switch (status) {
                case DANG_CHO -> "Đang chờ xử lý";
                case DA_XAC_NHAN -> "Đã xác nhận";
                case DANG_GIAO -> "Đang giao hàng";
                case DA_GIAO -> "Đã giao hàng";
                case YEU_CAU_TRA_HANG -> "Yêu cầu trả hàng";
                case TRA_HANG_THANH_CONG -> "Trả hàng thành công";
                case TRA_HANG_THAT_BAI -> "Trả hàng thất bại";
                case DA_HUY -> "Đã hủy";
                // Nếu muốn bắt default
                default -> "Trạng thái không xác định";
            };
        }

    
    /**
     * Chuyển đổi PaymentMethod enum sang text tiếng Việt
     */
    private String getPaymentMethodText(PaymentMethod method) {
        return switch (method) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case VNPAY -> "Chuyển khoản VNPay/VietQR";
            default -> "Phương thức không xác định";
        };
    }
    
    /**
     * Trừ stock sau khi thanh toán VNPay thành công
     */
    @Transactional
    private void deductProductStock(Order order) {
        log.info("Deducting stock for order: {}", order.getOrderCode());
        
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        List<UUID> variantIds = orderDetails.stream()
                .map(detail -> detail.getVariant().getId())
                .collect(Collectors.toList());
        
        List<ProductVariant> variants = productVariantRepository
                .findByIdInWithProductAndImages(variantIds);
        
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        
        for (OrderDetail detail : orderDetails) {
            ProductVariant variant = variantMap.get(detail.getVariant().getId());
            int newStock = variant.getStockQuantity() - detail.getQuantity();
            
            if (newStock < 0) {
                log.error("Insufficient stock for variant: {} - Available: {}, Required: {}",
                        variant.getId(), variant.getStockQuantity(), detail.getQuantity());
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            
            variant.setStockQuantity(newStock);
            log.debug("Variant {} stock: {} -> {}", variant.getId(), 
                    variant.getStockQuantity() + detail.getQuantity(), newStock);
        }
        
        productVariantRepository.saveAll(variants);
        log.info("Stock deducted successfully for {} items in order: {}", 
                orderDetails.size(), order.getOrderCode());
    }
    
    /**
     * Hoàn lại stock khi hủy đơn hàng (dùng cho COD hoặc đơn đã thanh toán VNPay)
     */
    @Transactional
    public void restoreProductStock(Order order) {
        log.info("Restoring stock for cancelled order: {}", order.getOrderCode());
        
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        List<UUID> variantIds = orderDetails.stream()
                .map(detail -> detail.getVariant().getId())
                .collect(Collectors.toList());
        
        List<ProductVariant> variants = productVariantRepository
                .findByIdInWithProductAndImages(variantIds);
        
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        
        for (OrderDetail detail : orderDetails) {
            ProductVariant variant = variantMap.get(detail.getVariant().getId());
            int newStock = variant.getStockQuantity() + detail.getQuantity();
            variant.setStockQuantity(newStock);
            log.debug("Variant {} stock restored: {} -> {}", variant.getId(), 
                    variant.getStockQuantity() - detail.getQuantity(), newStock);
        }
        
        productVariantRepository.saveAll(variants);
        log.info("Stock restored successfully for {} items in order: {}", 
                orderDetails.size(), order.getOrderCode());
    }
    
    /**
     * 🔥 Tăng sellNumber cho các sản phẩm khi đơn hàng DA_GIAO thành công
     * @return List of affected product IDs for ES sync
     */
    @Transactional
    private List<UUID> incrementProductSellNumberAndReturnProductIds(Order order) {
        log.info("📈 Incrementing sellNumber for order: {}", order.getOrderCode());
        
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        
        // Nhóm theo productId và tính tổng quantity
        Map<UUID, Integer> productQuantityMap = orderDetails.stream()
                .collect(Collectors.groupingBy(
                    detail -> detail.getVariant().getProduct().getId(),
                    Collectors.summingInt(OrderDetail::getQuantity)
                ));
        
        // Lấy tất cả products cần update
        List<UUID> productIds = new ArrayList<>(productQuantityMap.keySet());
        List<Product> products = productRepository.findAllById(productIds);
        
        // Cập nhật sellNumber
        for (Product product : products) {
            Integer soldQuantity = productQuantityMap.get(product.getId());
            Long currentSellNumber = product.getSellNumber() != null ? product.getSellNumber() : 0L;
            product.setSellNumber(currentSellNumber + soldQuantity);
            log.info("📊 Product {} sellNumber: {} -> {} (+{})", 
                product.getId(), currentSellNumber, product.getSellNumber(), soldQuantity);
        }
        
        productRepository.saveAll(products);
        log.info("✅ Updated sellNumber for {} products - will sync to ES after commit", products.size());
        
        return productIds; // Return for post-commit ES sync
    }
    
    /**
     * 🔥 Giảm sellNumber cho các sản phẩm khi đơn hàng DA_GIAO bị hủy/trả hàng
     * @return List of affected product IDs for ES sync
     */
    @Transactional
    private List<UUID> decrementProductSellNumberAndReturnProductIds(Order order) {
        log.info("📉 Decrementing sellNumber for cancelled/returned order: {}", order.getOrderCode());
        
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
        
        // Nhóm theo productId và tính tổng quantity
        Map<UUID, Integer> productQuantityMap = orderDetails.stream()
                .collect(Collectors.groupingBy(
                    detail -> detail.getVariant().getProduct().getId(),
                    Collectors.summingInt(OrderDetail::getQuantity)
                ));
        
        // Lấy tất cả products cần update
        List<UUID> productIds = new ArrayList<>(productQuantityMap.keySet());
        List<Product> products = productRepository.findAllById(productIds);
        
        // Giảm sellNumber (không cho âm)
        for (Product product : products) {
            Integer returnedQuantity = productQuantityMap.get(product.getId());
            Long currentSellNumber = product.getSellNumber() != null ? product.getSellNumber() : 0L;
            Long newSellNumber = Math.max(0L, currentSellNumber - returnedQuantity);
            product.setSellNumber(newSellNumber);
            log.info("📊 Product {} sellNumber: {} -> {} (-{})", 
                product.getId(), currentSellNumber, product.getSellNumber(), returnedQuantity);
        }
        
        productRepository.saveAll(products);
        log.info("✅ Decreased sellNumber for {} products - will sync to ES after commit", products.size());
        
        return productIds; // Return for post-commit ES sync
    }
    
    /**
     * Hủy các đơn hàng VNPay quá hạn (chưa thanh toán sau 15 phút)
     * 
     * @return Số lượng đơn hàng đã bị hủy
     */
    @Override
    @Transactional
    public int cancelExpiredVnpayOrders() {
        log.info("🕐 Checking for expired VNPay orders...");
        
        LocalDateTime fifteenMinutesAgo = getNowVietnam().minusMinutes(15);
        
        // Tìm tất cả đơn VNPay chưa thanh toán và đã quá 15 phút
        List<Order> expiredOrders = orderRepository.findExpiredVnpayOrders(
                PaymentMethod.VNPAY,
                PaymentStatus.CHUA_THANH_TOAN,
                OrderStatus.DANG_CHO,
                fifteenMinutesAgo
        );
        
        if (expiredOrders.isEmpty()) {
            log.info("✅ No expired VNPay orders found");
            return 0;
        }
        
        log.warn("⚠️ Found {} expired VNPay orders to cancel", expiredOrders.size());
        
        int cancelledCount = 0;
        
        for (Order order : expiredOrders) {
            try {
                // Cập nhật trạng thái đơn hàng
                order.setOrderStatus(OrderStatus.DA_HUY);
                order.setPaymentStatus(PaymentStatus.THAT_BAI);
                
                // Tạo lịch sử trạng thái
                createOrderStatusHistory(order, OrderStatus.DA_HUY, 
                        "Đơn hàng VNPay tự động hủy do quá hạn thanh toán (15 phút)");
                
                // Rollback vouchers (nếu có)
                List<OrderVoucher> orderVouchers = orderVoucherRepository.findByOrderIdWithVoucher(order.getId());
                for (OrderVoucher ov : orderVouchers) {
                    rollbackVoucher(ov.getVoucher().getId(), order.getUser().getId(), order.getId());
                }
                
                // ⭐ KHÔNG CẦN ROLLBACK STOCK - vì chưa bao giờ bị trừ!
                
                orderRepository.save(order);
                cancelledCount++;
                
                log.info("🗑️ Cancelled expired order: {} (created at: {})", 
                        order.getOrderCode(), order.getOrderDate());
                
            } catch (Exception e) {
                log.error("❌ Failed to cancel order: {} - Error: {}", 
                        order.getOrderCode(), e.getMessage(), e);
            }
        }
        
        log.info("✅ Cancelled {} expired VNPay orders", cancelledCount);
        return cancelledCount;
    }
    
    @Override
    public Page<OrderResponse> getOrdersWithFilter(String dateFilter, String statusFilter, Pageable pageable) {
        log.info("Fetching orders with dateFilter={}, statusFilter={}", dateFilter, statusFilter);
        
        // Xác định khoảng thời gian dựa trên dateFilter
        LocalDateTime startDate = null;
        LocalDateTime endDate = getNowVietnam();
        
        if (dateFilter != null && !dateFilter.equalsIgnoreCase("all")) {
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                    break;
                case "this_week":
                    // ⭐ Sửa: Lấy từ thứ 2 đầu tuần (DayOfWeek.MONDAY = 1)
                    startDate = LocalDateTime.now()
                            .with(java.time.DayOfWeek.MONDAY)
                            .toLocalDate()
                            .atStartOfDay();
                    break;
                case "this_month":
                    // ⭐ Sửa: Lấy từ ngày đầu tiên của tháng hiện tại thay vì 30 ngày trước
                    startDate = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
                    break;
                default:
                    startDate = null; // All dates
            }
        }
        
        Page<Order> orders;
        
        // Lọc theo cả ngày và trạng thái
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("all")) {
            // ⭐ Xử lý đặc biệt cho DA_HUY - bao gồm cả YEU_CAU_TRA_HANG, TRA_HANG_THANH_CONG và TRA_HANG_THAT_BAI
            if (statusFilter.equalsIgnoreCase("DA_HUY")) {
                List<OrderStatus> cancelledStatuses = Arrays.asList(
                    OrderStatus.DA_HUY, 
                    OrderStatus.YEU_CAU_TRA_HANG,
                    OrderStatus.TRA_HANG_THANH_CONG, 
                    OrderStatus.TRA_HANG_THAT_BAI
                );
                
                if (startDate != null) {
                    orders = orderRepository.findByOrderDateBetweenAndOrderStatusInOrderByOrderDateDesc(
                            startDate, endDate, cancelledStatuses, pageable);
                } else {
                    orders = orderRepository.findByOrderStatusInOrderByOrderDateDesc(cancelledStatuses, pageable);
                }
            } else {
                // Kiểm tra xem statusFilter là OrderStatus hay PaymentStatus
                boolean isOrderStatus = false;
                boolean isPaymentStatus = false;
                OrderStatus orderStatus = null;
                PaymentStatus paymentStatus = null;
                
                try {
                    orderStatus = OrderStatus.valueOf(statusFilter.toUpperCase());
                    isOrderStatus = true;
                } catch (IllegalArgumentException e) {
                    // Không phải OrderStatus, thử PaymentStatus
                }
                
                if (!isOrderStatus) {
                    try {
                        paymentStatus = PaymentStatus.valueOf(statusFilter.toUpperCase());
                        isPaymentStatus = true;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid status filter: {}", statusFilter);
                    }
                }
                
                // Query dựa trên loại status
                if (isOrderStatus) {
                    if (startDate != null) {
                        orders = orderRepository.findByOrderDateBetweenAndOrderStatusOrderByOrderDateDesc(
                                startDate, endDate, orderStatus, pageable);
                    } else {
                        orders = orderRepository.findByOrderStatusOrderByOrderDateDesc(orderStatus, pageable);
                    }
                } else if (isPaymentStatus) {
                    if (startDate != null) {
                        orders = orderRepository.findByOrderDateBetweenAndPaymentStatusOrderByOrderDateDesc(
                                startDate, endDate, paymentStatus, pageable);
                    } else {
                        orders = orderRepository.findByPaymentStatusOrderByOrderDateDesc(paymentStatus, pageable);
                    }
                } else {
                    // Invalid status, trả về tất cả
                    if (startDate != null) {
                        orders = orderRepository.findByOrderDateBetweenOrderByOrderDateDesc(
                                startDate, endDate, pageable);
                    } else {
                        orders = orderRepository.findAllByOrderByOrderDateDesc(pageable);
                    }
                }
            }
        } else {
            // Chỉ lọc theo ngày
            if (startDate != null) {
                orders = orderRepository.findByOrderDateBetweenOrderByOrderDateDesc(
                        startDate, endDate, pageable);
            } else {
                orders = orderRepository.findAllByOrderByOrderDateDesc(pageable);
            }
        }
        
        return orders.map(this::mapToOrderResponse);
    }
    
    @Override
    public Page<OrderResponse> searchOrders(String keyword, String tab, String dateFilter, int page, int size) {
        log.info("🔍 Tìm kiếm đơn hàng - Keyword: '{}', Tab: '{}', DateFilter: '{}', Page: {}, Size: {}", 
                keyword, tab, dateFilter, page, size);
        
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Page<Order> orders;
        
        // Chuẩn hóa keyword
        String normalizedKeyword = keyword.trim();
        
        // Xác định khoảng thời gian
        LocalDateTime startDate = null;
        LocalDateTime endDate = getNowVietnam();
        boolean hasDateFilter = false;
        
        if (dateFilter != null && !dateFilter.equalsIgnoreCase("all")) {
            hasDateFilter = true;
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                    break;
                case "this_week":
                    // ⭐ Sửa: Lấy từ thứ 2 đầu tuần (DayOfWeek.MONDAY = 1)
                    startDate = LocalDateTime.now()
                            .with(java.time.DayOfWeek.MONDAY)
                            .toLocalDate()
                            .atStartOfDay();
                    break;
                case "this_month":
                    // ⭐ Sửa: Lấy từ ngày đầu tiên của tháng hiện tại thay vì 30 ngày trước
                    startDate = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
                    break;
                default:
                    hasDateFilter = false;
            }
        }
        
        // Xử lý theo tab và dateFilter
        if (tab.equalsIgnoreCase("all")) {
            // Tìm kiếm tất cả đơn hàng
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.searchOrdersByDateRange(normalizedKeyword, startDate, endDate, pageable);
            } else {
                orders = orderRepository.searchAllOrders(normalizedKeyword, pageable);
            }
        } else if (tab.equalsIgnoreCase("DA_HUY")) {
            // ⭐ Xử lý đặc biệt cho DA_HUY - bao gồm cả YEU_CAU_TRA_HANG, TRA_HANG_THANH_CONG và TRA_HANG_THAT_BAI
            List<OrderStatus> cancelledStatuses = Arrays.asList(
                OrderStatus.DA_HUY,
                OrderStatus.YEU_CAU_TRA_HANG,
                OrderStatus.TRA_HANG_THANH_CONG,
                OrderStatus.TRA_HANG_THAT_BAI
            );
            
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.searchOrdersByOrderStatusInAndDateRange(
                        normalizedKeyword, cancelledStatuses, startDate, endDate, pageable);
            } else {
                orders = orderRepository.searchOrdersByOrderStatusIn(normalizedKeyword, cancelledStatuses, pageable);
            }
        } else {
            // Chuyển đổi tab thành OrderStatus hoặc PaymentStatus
            String statusEnum = convertTabToStatusEnum(tab);
            
            // Kiểm tra xem là OrderStatus hay PaymentStatus
            boolean isOrderStatus = false;
            boolean isPaymentStatus = false;
            OrderStatus orderStatus = null;
            PaymentStatus paymentStatus = null;
            
            try {
                orderStatus = OrderStatus.valueOf(statusEnum);
                isOrderStatus = true;
            } catch (IllegalArgumentException e) {
                // Không phải OrderStatus
            }
            
            if (!isOrderStatus) {
                try {
                    paymentStatus = PaymentStatus.valueOf(statusEnum);
                    isPaymentStatus = true;
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tab value: {}", tab);
                }
            }
            
            // Query theo loại status và dateFilter
            if (isOrderStatus) {
                if (hasDateFilter && startDate != null) {
                    orders = orderRepository.searchOrdersByOrderStatusAndDateRange(
                            normalizedKeyword, orderStatus, startDate, endDate, pageable);
                } else {
                    orders = orderRepository.searchOrdersByOrderStatus(normalizedKeyword, orderStatus, pageable);
                }
            } else if (isPaymentStatus) {
                if (hasDateFilter && startDate != null) {
                    orders = orderRepository.searchOrdersByPaymentStatusAndDateRange(
                            normalizedKeyword, paymentStatus, startDate, endDate, pageable);
                } else {
                    orders = orderRepository.searchOrdersByPaymentStatus(normalizedKeyword, paymentStatus, pageable);
                }
            } else {
                // Invalid tab, tìm tất cả
                if (hasDateFilter && startDate != null) {
                    orders = orderRepository.searchOrdersByDateRange(normalizedKeyword, startDate, endDate, pageable);
                } else {
                    orders = orderRepository.searchAllOrders(normalizedKeyword, pageable);
                }
            }
        }
        
        log.info("✅ Tìm thấy {} đơn hàng", orders.getTotalElements());
        return orders.map(this::mapToOrderResponse);
    }
    
    /**
     * Chuyển đổi tab value thành enum constant name
     * ví dụ: dang_cho -> DANG_CHO
     */
    private String convertTabToStatusEnum(String tab) {
        return tab.toUpperCase();
    }
    
    @Override
    public OrderResponse getOrderDetail(UUID orderId) {
        log.info("📑 Lấy thông tin chi tiết đơn hàng: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // Map to response với đầy đủ thông tin
        OrderResponse response = mapToOrderResponse(order);
        
        // Load thêm status history
        List<OrderStatusHistoryResponse> statusHistory = getOrderStatusHistory(orderId);
        response.setStatusHistory(statusHistory);
        
        // Tính tổng số loại sản phẩm và tổng số lượng
        if (response.getOrderItems() != null && !response.getOrderItems().isEmpty()) {
            response.setTotalItems(response.getOrderItems().size());
            response.setTotalQuantity(
                response.getOrderItems().stream()
                    .mapToInt(OrderItemResponse::getQuantity)
                    .sum()
            );
        } else {
            response.setTotalItems(0);
            response.setTotalQuantity(0);
        }
        
        return response;
    }
    
    @Override
    public OrderResponse updateOrderStatusWithCashback(UUID orderId, String newStatus, String note) {
        log.info("🔄 Cập nhật trạng thái đơn hàng với cashback - OrderId: {}, NewStatus: {}", orderId, newStatus);
        
        // Execute transactional logic and collect products to sync
        List<UUID> productsToSync = updateOrderStatusTransactional(orderId, newStatus, note);
        
        // ⚡ SYNC ELASTICSEARCH AFTER TRANSACTION COMMIT
        if (!productsToSync.isEmpty() && elasticsearchSyncService != null) {
            for (UUID productId : productsToSync) {
                try {
                    elasticsearchSyncService.syncProduct(productId);
                    log.info("🔄 [POST-COMMIT] Synced product {} to Elasticsearch", productId);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to sync product {} to Elasticsearch: {}", productId, e.getMessage());
                }
            }
        }
        
        // Return final order response
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderResponse(order);
    }
    
    /**
     * 🔒 TRANSACTIONAL CORE LOGIC - Returns list of product IDs that need ES sync
     */
    @Transactional
    private List<UUID> updateOrderStatusTransactional(UUID orderId, String newStatus, String note) {
        List<UUID> productsToSync = new ArrayList<>();
        
        // Lấy đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        OrderStatus oldStatus = order.getOrderStatus();
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        OrderStatus orderStatus;
        
        try {
            orderStatus = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // ⭐ CRITICAL: Prevent duplicate business logic if admin updates same status multiple times
        if (oldStatus == orderStatus) {
            log.warn("⚠️ Trạng thái không thay đổi ({} -> {}) - Bỏ qua xử lý business logic", oldStatus, orderStatus);
            
            // Still create history for tracking
            String statusNote = note != null ? note : "Cập nhật lại trạng thái " + orderStatus.name();
            createOrderStatusHistory(order, orderStatus, statusNote);
            
            orderRepository.save(order);
            return productsToSync; // Empty list
        }
        
        // ⭐ VALIDATE: Không cho revert về trạng thái trước đó
        validateOrderStatusTransition(oldStatus, orderStatus);
        
        log.info("✅ Chuyển trạng thái hợp lệ: {} -> {}", oldStatus, orderStatus);
        
        // Lấy user
        User user = order.getUser();
        
        // ⭐ CASE 1: ORDER DELIVERED (DA_GIAO) - Increment sellNumber and award loyalty points
        if (orderStatus == OrderStatus.DA_GIAO) {
            log.info("✅ Đơn hàng đã giao thành công");
            
            // 📅 Lưu ngày giao hàng thực tế
            order.setActualDeliveryDate(getNowVietnam());
            log.info("📅 Set actualDeliveryDate: {}", order.getActualDeliveryDate());
            
            // 🔥 Cập nhật sellNumber cho các sản phẩm trong đơn hàng
            List<UUID> affectedProducts = incrementProductSellNumberAndReturnProductIds(order);
            productsToSync.addAll(affectedProducts);
            
            // ⭐ CỘNG ĐIỂM TÍCH LŨY khi DA_GIAO:
            // - VNPay (đã thanh toán trước): Cộng ngay
            // - COD đã thanh toán: Cộng ngay
            // - COD chưa thanh toán: Chờ DA_THANH_TOAN
            boolean shouldAwardLoyaltyPoints = false;
            
            if (order.getPaymentMethod() == PaymentMethod.VNPAY && 
                order.getPaymentStatus() == PaymentStatus.DA_THANH_TOAN) {
                shouldAwardLoyaltyPoints = true;
                log.info("💳 [VNPAY] Đơn đã thanh toán - Cộng điểm tích lũy ngay");
            } else if (order.getPaymentMethod() == PaymentMethod.COD && 
                       order.getPaymentStatus() == PaymentStatus.DA_THANH_TOAN) {
                shouldAwardLoyaltyPoints = true;
                log.info("💵 [COD] Đơn đã thanh toán - Cộng điểm tích lũy ngay");
            } else {
                log.info("💵 [COD] Đơn chưa thanh toán - Chờ DA_THANH_TOAN mới cộng điểm");
            }
            
            // Kiểm tra history để tránh trùng lặp
            if (shouldAwardLoyaltyPoints) {
                boolean alreadyAwarded = orderStatusHistoryRepository.existsByOrderIdAndStatus(
                    order.getId(), "LOYALTY_POINTS_AWARDED");
                
                if (!alreadyAwarded) {
                    awardLoyaltyPointsToUser(order, user);
                } else {
                    log.info("⚠️ Điểm tích lũy đã được cộng trước đó - Bỏ qua");
                }
            }
            
            // Note: Rank points are stored in Order entity but not processed
            if (order.getRankPoints() != null && order.getRankPoints().compareTo(BigDecimal.ZERO) > 0) {
                log.info("⭐ Rank points: {} (chỉ lưu trữ, chưa xử lý)", order.getRankPoints());
            }
        }
        
        // ⭐ CASE 2: ORDER RETURNED SUCCESSFULLY (CHỈ TRA_HANG_THANH_CONG mới rollback)
        if (orderStatus == OrderStatus.TRA_HANG_THANH_CONG) {
            
            log.info("🔄 Đơn hàng trả hàng thành công - Xử lý rollback điểm, kho, voucher");
            
            // 0️⃣ Rollback sellNumber nếu đơn hàng đã DA_GIAO trước đó
            if (oldStatus == OrderStatus.DA_GIAO) {
                List<UUID> affectedProducts = decrementProductSellNumberAndReturnProductIds(order);
                productsToSync.addAll(affectedProducts);
            }
            
            // 1️⃣ Restore product stock (if it was deducted)
            if (oldPaymentStatus == PaymentStatus.DA_THANH_TOAN || order.getPaymentMethod() == PaymentMethod.COD) {
                restoreProductStock(order);
                log.info("🔄 Rollback stock cho đơn hàng: {}", order.getOrderCode());
            }
            
            // 3️⃣ Rollback vouchers
            List<OrderVoucher> orderVouchers = orderVoucherRepository.findByOrderIdWithVoucher(order.getId());
            for (OrderVoucher ov : orderVouchers) {
                rollbackVoucher(ov.getVoucher().getId(), user.getId(), order.getId());
            }
            
            // 4️⃣ Handle loyalty points and total payment refund ONLY if payment was completed
            if (oldPaymentStatus == PaymentStatus.DA_THANH_TOAN) {
                log.info("💳 Đơn đã thanh toán - Xử lý hoàn tiền");
                
                // 🔥 NEW: Gọi API hoàn tiền VNPay nếu TRA_HANG_THANH_CONG và là đơn VNPay
                if (orderStatus == OrderStatus.TRA_HANG_THANH_CONG && 
                    order.getPaymentMethod() == PaymentMethod.VNPAY) {
                    
                    log.info("💳 [VNPAY REFUND] Xử lý hoàn tiền cho đơn trả hàng thành công: {}", order.getOrderCode());
                    
                    if (order.getVnpayTransactionNo() == null || order.getVnpayTransactionNo().isEmpty()) {
                        log.error("⚠️ Không tìm thấy vnpayTransactionNo cho đơn hàng {}", order.getOrderCode());
                        
                        // Gửi thông báo cho admin
                        notificationDatabaseService.createNotification(
                            user.getId(),
                            "refund_error",
                            "⚠️ Lỗi hoàn tiền VNPay",
                            String.format("Đơn hàng #%s trả hàng thành công nhưng thiếu mã giao dịch VNPay để hoàn tiền. Vui lòng xử lý thủ công.", 
                                order.getOrderCode()),
                            "/orders/" + order.getId(),
                            com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                        );
                    } else {
                        // Gọi API hoàn tiền VNPay
                        String refundReason = note != null && !note.isEmpty() 
                            ? "Trả hàng thành công: " + note 
                            : "Trả hàng thành công";
                        
                        try {
                            boolean refundSuccess = vnpayService.refundTransaction(
                                order.getOrderCode(),
                                order.getOrderDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                                order.getTotalPayment(),
                                "02", // Full refund
                                "SYSTEM_AUTO_REFUND",
                                refundReason,
                                order.getVnpayTransactionNo()
                            );
                            
                            if (refundSuccess) {
                                log.info("✅ [VNPAY REFUND] Hoàn tiền thành công cho đơn hàng: {}", order.getOrderCode());
                                
                                // Gửi thông báo cho admin
                                notificationDatabaseService.createNotification(
                                    user.getId(),
                                    "refund_success",
                                    "✅ Hoàn tiền VNPay thành công",
                                    String.format("Đơn hàng #%s đã được hoàn tiền %,.0f đ qua VNPay.", 
                                        order.getOrderCode(), order.getTotalPayment()),
                                    "/orders/" + order.getId(),
                                    com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                                );
                                
                                // Gửi thông báo cho khách hàng
                                notificationDatabaseService.createNotificationForCustomer(
                                    user.getId(),
                                    "refund_success",
                                    "💰 Đơn hàng đã được hoàn tiền",
                                    String.format("Đơn hàng #%s đã được duyệt trả hàng và hoàn tiền %,.0f đ về tài khoản VNPay của bạn.", 
                                        order.getOrderCode(), order.getTotalPayment()),
                                    "/orders/" + order.getId()
                                );
                            } else {
                                log.error("❌ [VNPAY REFUND] Hoàn tiền thất bại cho đơn hàng: {}", order.getOrderCode());
                                
                                // Gửi thông báo cho admin
                                notificationDatabaseService.createNotification(
                                    user.getId(),
                                    "refund_failed",
                                    "❌ Hoàn tiền VNPay thất bại",
                                    String.format("Đơn hàng #%s trả hàng thành công nhưng hoàn tiền VNPay thất bại. Vui lòng kiểm tra và xử lý thủ công.", 
                                        order.getOrderCode()),
                                    "/orders/" + order.getId(),
                                    com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                                );
                            }
                        } catch (Exception e) {
                            log.error("❌ [VNPAY REFUND] Exception khi hoàn tiền đơn hàng {}: {}", order.getOrderCode(), e.getMessage(), e);
                            
                            // Gửi thông báo cho admin
                            notificationDatabaseService.createNotification(
                                user.getId(),
                                "refund_exception",
                                "❌ Lỗi hệ thống hoàn tiền VNPay",
                                String.format("Đơn hàng #%s trả hàng thành công nhưng gặp lỗi khi gọi API hoàn tiền VNPay: %s", 
                                    order.getOrderCode(), e.getMessage()),
                                "/orders/" + order.getId(),
                                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                            );
                        }
                    }
                }
                
                // Change payment status
                order.setPaymentStatus(PaymentStatus.THAT_BAI);
                
                // 4a. Hoàn lại điểm loyalty đã dùng để giảm giá (order.getLoyaltyPoints)
                if (order.getLoyaltyPoints() != null && order.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                    BigDecimal refundPoints = order.getLoyaltyPoints();
                    
                    user.setLoyaltyPoints(currentLoyaltyPoints.add(refundPoints));
                    log.info("🔺 [{}] Hoàn {} điểm loyalty (đã dùng giảm giá) cho user {}. Cũ: {}, Mới: {}", 
                        order.getPaymentMethod(), refundPoints, user.getId(), currentLoyaltyPoints, user.getLoyaltyPoints());
                }
                
                // 4b. Trừ lại điểm tích lũy đã cộng khi DA_GIAO (nếu đã được cộng)
                boolean wasLoyaltyAwarded = orderStatusHistoryRepository.existsByOrderIdAndStatus(
                    order.getId(), "LOYALTY_POINTS_AWARDED");
                
                if (wasLoyaltyAwarded) {
                    // Điểm tích lũy = totalPayment × 0.02 (2%)
                    BigDecimal pointsToDeduct = order.getTotalPayment()
                            .multiply(new BigDecimal("0.02"))
                            .setScale(0, java.math.RoundingMode.FLOOR);
                    BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                    BigDecimal newLoyaltyPoints = currentLoyaltyPoints.subtract(pointsToDeduct);
                    
                    if (newLoyaltyPoints.compareTo(BigDecimal.ZERO) < 0) {
                        newLoyaltyPoints = BigDecimal.ZERO;
                    }
                    
                    user.setLoyaltyPoints(newLoyaltyPoints);
                    log.info("🔻 Trừ lại {} điểm tích lũy (2% của {}) đã cộng. Cũ: {}, Mới: {}", 
                        pointsToDeduct, order.getTotalPayment(), currentLoyaltyPoints, user.getLoyaltyPoints());
                    
                    // Ghi history rollback
                    OrderStatusHistory rollbackHistory = OrderStatusHistory.builder()
                            .order(order)
                            .status("LOYALTY_POINTS_ROLLBACK")
                            .note("Trừ lại " + pointsToDeduct + " điểm tích lũy (2% của " + order.getTotalPayment() + ") do hủy/trả hàng")
                            .build();
                    orderStatusHistoryRepository.save(rollbackHistory);
                }
                
                // 4c. Subtract from user's total payment amount
                BigDecimal currentTotalPayment = user.getTotalPaymentAmount() != null ? user.getTotalPaymentAmount() : BigDecimal.ZERO;
                BigDecimal newTotalPayment = currentTotalPayment.subtract(order.getTotalPayment());
                if (newTotalPayment.compareTo(BigDecimal.ZERO) < 0) {
                    newTotalPayment = BigDecimal.ZERO;
                }
                user.setTotalPaymentAmount(newTotalPayment);
                log.info("💸 Trừ {} khỏi tổng tiền mua. Cũ: {}, Mới: {}", 
                    order.getTotalPayment(), currentTotalPayment, user.getTotalPaymentAmount());
                
                userRepository.save(user);
            } else if (order.getPaymentMethod() == PaymentMethod.COD && oldPaymentStatus == PaymentStatus.CHUA_THANH_TOAN) {
                // For COD orders that were never paid, still need to refund loyalty points used for discount
                log.info("💵 [COD] Đơn chưa thanh toán - hoàn lại điểm loyalty đã dùng giảm giá");
                
                if (order.getLoyaltyPoints() != null && order.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                    BigDecimal refundPoints = order.getLoyaltyPoints();
                    
                    user.setLoyaltyPoints(currentLoyaltyPoints.add(refundPoints));
                    log.info("🔺 [COD] Hoàn {} điểm loyalty cho user {}. Cũ: {}, Mới: {}", 
                        refundPoints, user.getId(), currentLoyaltyPoints, user.getLoyaltyPoints());
                    
                    userRepository.save(user);
                }
            }
        }
        
        // Cập nhật trạng thái đơn hàng
        order.setOrderStatus(orderStatus);
        
        // Tạo lịch sử trạng thái
        String statusNote = note != null ? note : "Cập nhật trạng thái thành " + orderStatus.name();
        createOrderStatusHistory(order, orderStatus, statusNote);
        
        order = orderRepository.save(order);
        
        log.info("✅ Cập nhật trạng thái thành công: {} -> {}", oldStatus, orderStatus);
        
        // 📢 GỬI THÔNG BÁO CHO CUSTOMER khi trạng thái đơn hàng thay đổi
        try {
            String notificationTitle = getOrderStatusNotificationTitle(orderStatus, order.getOrderCode());
            String notificationMessage = getOrderStatusNotificationMessage(orderStatus, order.getOrderCode(), note);
            
            notificationDatabaseService.createNotificationForCustomer(
                user.getId(),
                "cap_nhat_don_hang",
                notificationTitle,
                notificationMessage,
                "/orders/" + order.getId()
            );
            log.info("📢 Đã gửi thông báo cập nhật trạng thái đơn hàng cho customer: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Lỗi gửi thông báo cho customer: {}", e.getMessage());
        }
        
        return productsToSync;
    }
    
    /**
     * Helper: Tạo tiêu đề thông báo cho customer dựa trên trạng thái đơn hàng
     */
    private String getOrderStatusNotificationTitle(OrderStatus status, String orderCode) {
        return switch (status) {
            case DA_XAC_NHAN -> "Đơn hàng " + orderCode + " đã được xác nhận";
            case DANG_GIAO -> "Đơn hàng " + orderCode + " đang được giao";
            case DA_GIAO -> "Đơn hàng " + orderCode + " đã giao thành công";
            case DA_HUY -> "Đơn hàng " + orderCode + " đã bị hủy";
            case TRA_HANG_THANH_CONG -> "Yêu cầu trả hàng " + orderCode + " đã được chấp nhận";
            case TRA_HANG_THAT_BAI -> "Yêu cầu trả hàng " + orderCode + " bị từ chối";
            default -> "Cập nhật đơn hàng " + orderCode;
        };
    }
    
    /**
     * Helper: Tạo nội dung thông báo cho customer dựa trên trạng thái đơn hàng
     */
    private String getOrderStatusNotificationMessage(OrderStatus status, String orderCode, String note) {
        String baseMessage = switch (status) {
            case DA_XAC_NHAN -> "Đơn hàng của bạn đã được xác nhận và đang chuẩn bị.";
            case DANG_GIAO -> "Đơn hàng của bạn đang trên đường giao đến bạn.";
            case DA_GIAO -> "Đơn hàng đã được giao thành công. Cảm ơn bạn đã mua hàng!";
            case DA_HUY -> "Đơn hàng đã bị hủy.";
            case TRA_HANG_THANH_CONG -> "Yêu cầu trả hàng của bạn đã được chấp nhận. Chúng tôi sẽ hoàn tiền trong thời gian sớm nhất.";
            case TRA_HANG_THAT_BAI -> "Yêu cầu trả hàng của bạn không được chấp nhận.";
            default -> "Trạng thái đơn hàng đã được cập nhật.";
        };
        
        // Thêm ghi chú nếu có
        if (note != null && !note.trim().isEmpty()) {
            baseMessage += " Ghi chú: " + note;
        }
        
        return baseMessage;
    }
    
    /**
     * Helper: Cộng điểm tích lũy (loyaltyPoints) cho user khi đơn hàng hoàn thành
     * Điều kiện: Đơn hàng đã giao (DA_GIAO) VÀ đã thanh toán (DA_THANH_TOAN)
     * 
     * <p><b>Công thức:</b></p>
     * <ul>
     *   <li>Điểm tích lũy = totalPayment × 0.02 (2% của giá trị đơn hàng)</li>
     * </ul>
     * 
     * <p><b>Lưu ý:</b> Tổng tiền mua (totalPaymentAmount) được cộng riêng trong updatePaymentStatus</p>
     * 
     * @param order Đơn hàng
     * @param user User cần cộng điểm
     */
    private void awardLoyaltyPointsToUser(Order order, User user) {
        // Tính điểm tích lũy = totalPayment * 0.02 (2% của giá trị đơn hàng)
        BigDecimal pointsToAward = order.getTotalPayment()
                .multiply(new BigDecimal("0.02"))
                .setScale(0, java.math.RoundingMode.FLOOR); // Làm tròn xuống, không có số lẻ
        
        if (pointsToAward == null || pointsToAward.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("⚠️ Không có điểm tích lũy để cộng cho đơn hàng: {}", order.getOrderCode());
            return;
        }
        
        // Cộng điểm tích lũy (2% của totalPayment)
        BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
        user.setLoyaltyPoints(currentLoyaltyPoints.add(pointsToAward));
        
        log.info("🎁 [{}] Cộng {} điểm tích lũy (2% của {}) cho user {}. Cũ: {}, Mới: {}", 
            order.getPaymentMethod(), pointsToAward, order.getTotalPayment(), user.getId(), 
            currentLoyaltyPoints, user.getLoyaltyPoints());
        
        userRepository.save(user);
        
        // Ghi history để tránh trùng lặp
        OrderStatusHistory awardHistory = OrderStatusHistory.builder()
                .order(order)
                .status("LOYALTY_POINTS_AWARDED")
                .note("Cộng " + pointsToAward + " điểm tích lũy (2% của " + order.getTotalPayment() + ")")
                .build();
        orderStatusHistoryRepository.save(awardHistory);
        
        log.info("📝 Đã ghi lịch sử cộng điểm tích lũy cho đơn hàng: {}", order.getOrderCode());
    }

    /**
     * ⭐ Validate chuyển đổi trạng thái đơn hàng - Không cho revert về trạng thái trước đó
     * 
     * <p><b>Flow nghiệp vụ hợp lệ:</b></p>
     * <ul>
     *   <li>DANG_CHO → DA_XAC_NHAN</li>
     *   <li>DA_XAC_NHAN → DANG_GIAO</li>
     *   <li>DANG_GIAO → DA_GIAO</li>
     *   <li>DA_GIAO → YEU_CAU_TRA_HANG</li>
     *   <li>YEU_CAU_TRA_HANG → TRA_HANG_THANH_CONG hoặc TRA_HANG_THAT_BAI</li>
     *   <li>Mọi trạng thái (trừ final states) → DA_HUY</li>
     * </ul>
     * 
     * @param oldStatus Trạng thái hiện tại
     * @param newStatus Trạng thái muốn chuyển đến
     * @throws AppException Nếu chuyển đổi không hợp lệ
     */
    private void validateOrderStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        // Define valid transitions theo thứ tự trong OrderStatus enum
        Map<OrderStatus, List<OrderStatus>> validTransitions = new HashMap<>();
        
        // DANG_CHO có thể chuyển sang DA_XAC_NHAN hoặc DA_HUY
        validTransitions.put(OrderStatus.DANG_CHO, 
            List.of(OrderStatus.DA_XAC_NHAN, OrderStatus.DA_HUY));
        
        // DA_XAC_NHAN có thể chuyển sang DANG_GIAO hoặc DA_HUY
        validTransitions.put(OrderStatus.DA_XAC_NHAN, 
            List.of(OrderStatus.DANG_GIAO, OrderStatus.DA_HUY));
        
        // DANG_GIAO có thể chuyển sang DA_GIAO hoặc DA_HUY
        validTransitions.put(OrderStatus.DANG_GIAO, 
            List.of(OrderStatus.DA_GIAO, OrderStatus.DA_HUY));
        
        // DA_GIAO chỉ có thể chuyển sang YEU_CAU_TRA_HANG (không cho hủy nữa)
        validTransitions.put(OrderStatus.DA_GIAO, 
            List.of(OrderStatus.YEU_CAU_TRA_HANG));
        
        // YEU_CAU_TRA_HANG có thể chuyển sang TRA_HANG_THANH_CONG hoặc TRA_HANG_THAT_BAI
        validTransitions.put(OrderStatus.YEU_CAU_TRA_HANG, 
            List.of(OrderStatus.TRA_HANG_THANH_CONG, OrderStatus.TRA_HANG_THAT_BAI));
        
        // TRA_HANG_THANH_CONG, TRA_HANG_THAT_BAI, DA_HUY là trạng thái cuối - không cho chuyển nữa
        validTransitions.put(OrderStatus.TRA_HANG_THANH_CONG, List.of());
        validTransitions.put(OrderStatus.TRA_HANG_THAT_BAI, List.of());
        validTransitions.put(OrderStatus.DA_HUY, List.of());
        
        // Kiểm tra xem có transition hợp lệ không
        List<OrderStatus> allowedTransitions = validTransitions.get(oldStatus);
        
        if (allowedTransitions == null || allowedTransitions.isEmpty()) {
            log.error("❌ Trạng thái {} là trạng thái cuối, không thể chuyển sang {}", 
                oldStatus, newStatus);
            throw new AppException(ErrorCode.INVALID_REQUEST, 
                "Trạng thái không hợp lệ. Đơn hàng ở trạng thái '" + oldStatus.name() + 
                "' là trạng thái cuối, không thể thay đổi.");
        }
        
        if (!allowedTransitions.contains(newStatus)) {
            log.error("❌ Không thể chuyển từ {} sang {}", oldStatus, newStatus);
            throw new AppException(ErrorCode.INVALID_REQUEST, 
                "Trạng thái không hợp lệ, không được trở lại trạng thái trước đó. " +
                "Trạng thái hiện tại: " + oldStatus.name() + ", " +
                "Trạng thái hợp lệ: " + allowedTransitions.stream()
                    .map(OrderStatus::name)
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
    }
    
    /**
     * ⭐ Validate chuyển đổi trạng thái thanh toán - Không cho revert về trạng thái trước đó
     * 
     * <p><b>Flow nghiệp vụ hợp lệ:</b></p>
     * <ul>
     *   <li>CHUA_THANH_TOAN → DA_THANH_TOAN (thanh toán thành công)</li>
     *   <li>CHUA_THANH_TOAN → THAT_BAI (thanh toán thất bại)</li>
     *   <li>THAT_BAI → Không cho chuyển (trạng thái cuối)</li>
     *   <li>DA_THANH_TOAN → Không cho chuyển (trạng thái cuối)</li>
     * </ul>
     * 
     * @param oldStatus Trạng thái thanh toán hiện tại
     * @param newStatus Trạng thái thanh toán muốn chuyển đến
     * @throws AppException Nếu chuyển đổi không hợp lệ
     */
    private void validatePaymentStatusTransition(PaymentStatus oldStatus, PaymentStatus newStatus) {
        // Define valid transitions
        Map<PaymentStatus, List<PaymentStatus>> validTransitions = new HashMap<>();
        
        // CHUA_THANH_TOAN có thể chuyển sang DA_THANH_TOAN hoặc THAT_BAI
        validTransitions.put(PaymentStatus.CHUA_THANH_TOAN, 
            List.of(PaymentStatus.DA_THANH_TOAN, PaymentStatus.THAT_BAI));
        
        // THAT_BAI là trạng thái cuối - không cho chuyển nữa
        validTransitions.put(PaymentStatus.THAT_BAI, List.of());
        
        // DA_THANH_TOAN là trạng thái cuối - không cho chuyển nữa
        validTransitions.put(PaymentStatus.DA_THANH_TOAN, List.of());
        
        // Kiểm tra xem có transition hợp lệ không
        List<PaymentStatus> allowedTransitions = validTransitions.get(oldStatus);
        
        if (allowedTransitions == null || allowedTransitions.isEmpty()) {
            log.error("❌ Không thể chuyển từ trạng thái thanh toán cuối: {}", oldStatus.name());
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Trạng thái thanh toán không hợp lệ: '" + oldStatus.name() + 
                "' là trạng thái cuối, không thể thay đổi.");
        }
        
        if (!allowedTransitions.contains(newStatus)) {
            log.error("❌ Không thể chuyển trạng thái thanh toán từ {} sang {}", oldStatus, newStatus);
            throw new AppException(ErrorCode.INVALID_REQUEST, 
                "Trạng thái thanh toán không hợp lệ, không được trở lại trạng thái trước đó. " +
                "Trạng thái hiện tại: " + oldStatus.name() + ", " +
                "Trạng thái hợp lệ: " + allowedTransitions.stream()
                    .map(PaymentStatus::name)
                    .collect(java.util.stream.Collectors.joining(", ")));
        }
    }
    
    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(UUID orderId, String newPaymentStatus, String note) {
        log.info("💳 Cập nhật trạng thái thanh toán - OrderId: {}, NewPaymentStatus: {}", orderId, newPaymentStatus);
        
        // Validate payment status
        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.valueOf(newPaymentStatus);
        } catch (IllegalArgumentException e) {
            log.error("❌ Trạng thái thanh toán không hợp lệ: {}", newPaymentStatus);
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + newPaymentStatus);
        }
        
        // Find order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("❌ Không tìm thấy đơn hàng: {}", orderId);
                    return new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + orderId);
                });
        
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        
        // Check if status is already the same
        if (oldPaymentStatus == paymentStatus) {
            log.warn("⚠️ Trạng thái thanh toán đã là: {}", paymentStatus.name());
            throw new IllegalStateException("Đơn hàng đã ở trạng thái thanh toán: " + paymentStatus.name());
        }
        
        // ⭐ VALIDATE: Kiểm tra flow chuyển đổi trạng thái thanh toán hợp lệ
        validatePaymentStatusTransition(oldPaymentStatus, paymentStatus);
        
        log.info("✅ Chuyển trạng thái thanh toán hợp lệ: {} -> {}", oldPaymentStatus, paymentStatus);
        
        User user = order.getUser();
        
        // ⭐ CASE 1: PAYMENT CONFIRMED (CHUA_THANH_TOAN -> DA_THANH_TOAN)
        if (oldPaymentStatus != PaymentStatus.DA_THANH_TOAN && paymentStatus == PaymentStatus.DA_THANH_TOAN) {
            log.info("✅ Thanh toán được xác nhận - Xử lý trừ điểm loyalty (VNPay) và cộng tổng tiền");
            
            // 1️⃣ Deduct loyalty points ONLY for VNPay orders (COD already deducted at order creation)
            if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
                if (order.getLoyaltyPoints() != null && order.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                    BigDecimal usedPoints = order.getLoyaltyPoints();
                    
                    if (currentLoyaltyPoints.compareTo(usedPoints) < 0) {
                        throw new AppException(ErrorCode.INSUFFICIENT_LOYALTY_POINTS, 
                            "User có " + currentLoyaltyPoints + " điểm nhưng đơn hàng cần " + usedPoints + " điểm");
                    }
                    
                    user.setLoyaltyPoints(currentLoyaltyPoints.subtract(usedPoints));
                    log.info("🔻 [VNPAY] Đã trừ {} điểm loyalty từ user {}. Cũ: {}, Mới: {}", 
                        usedPoints, user.getId(), currentLoyaltyPoints, user.getLoyaltyPoints());
                }
            } else {
                log.info("💵 [COD] Loyalty points đã trừ khi tạo đơn - bỏ qua");
            }
            
            // 2️⃣ Add to user's total payment amount
            BigDecimal currentTotalPayment = user.getTotalPaymentAmount() != null ? user.getTotalPaymentAmount() : BigDecimal.ZERO;
            user.setTotalPaymentAmount(currentTotalPayment.add(order.getTotalPayment()));
            log.info("💰 Đã cộng {} vào tổng tiền. Cũ: {}, Mới: {}", 
                order.getTotalPayment(), currentTotalPayment, user.getTotalPaymentAmount());
            
            userRepository.save(user);
            
            // 🛒 Xóa các sản phẩm đã đặt hàng ra khỏi giỏ hàng (chỉ cho VNPay - COD đã xóa khi tạo đơn)
            if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
                removeOrderedItemsFromCart(order.getId(), user.getId());
            }
            
            // 3️⃣ Cộng điểm tích lũy nếu đơn hàng đã giao (DA_GIAO) + COD vừa thanh toán
            // VNPay đã được xử lý trong updateOrderStatusTransactional khi DA_GIAO
            if (order.getPaymentMethod() == PaymentMethod.COD && 
                order.getOrderStatus() == OrderStatus.DA_GIAO) {
                
                boolean alreadyAwarded = orderStatusHistoryRepository.existsByOrderIdAndStatus(
                    order.getId(), "LOYALTY_POINTS_AWARDED");
                
                if (!alreadyAwarded) {
                    awardLoyaltyPointsToUser(order, user);
                    log.info("💵 [COD] Đơn đã giao + vừa thanh toán - Đã cộng điểm tích lũy");
                } else {
                    log.info("⚠️ [COD] Điểm tích lũy đã được cộng trước đó - Bỏ qua");
                }
            }
        }
        
        // ⭐ CASE 2: ORDER CANCELLED OR RETURNED (DA_THANH_TOAN -> other status)
        if (oldPaymentStatus == PaymentStatus.DA_THANH_TOAN && paymentStatus != PaymentStatus.DA_THANH_TOAN) {
            log.info("🔄 Thanh toán bị hủy/hoàn trả - Hoàn lại điểm loyalty và trừ tổng tiền");
            
            // 1️⃣ Refund loyalty points (for both COD and VNPay)
            if (order.getLoyaltyPoints() != null && order.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentLoyaltyPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : BigDecimal.ZERO;
                BigDecimal refundPoints = order.getLoyaltyPoints();
                
                user.setLoyaltyPoints(currentLoyaltyPoints.add(refundPoints));
                log.info("🔺 [{}] Đã hoàn {} điểm loyalty cho user {}. Cũ: {}, Mới: {}", 
                    order.getPaymentMethod(), refundPoints, user.getId(), currentLoyaltyPoints, user.getLoyaltyPoints());
            }
            
            // 2️⃣ Subtract from user's total payment amount
            BigDecimal currentTotalPayment = user.getTotalPaymentAmount() != null ? user.getTotalPaymentAmount() : BigDecimal.ZERO;
            BigDecimal newTotalPayment = currentTotalPayment.subtract(order.getTotalPayment());
            // Ensure it doesn't go negative
            if (newTotalPayment.compareTo(BigDecimal.ZERO) < 0) {
                newTotalPayment = BigDecimal.ZERO;
            }
            user.setTotalPaymentAmount(newTotalPayment);
            log.info("💸 Đã trừ {} khỏi tổng tiền. Cũ: {}, Mới: {}", 
                order.getTotalPayment(), currentTotalPayment, user.getTotalPaymentAmount());
            
            userRepository.save(user);
        }
        
        // Update payment status
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
        
        // Create status history
        String historyNote = note != null ? note : "Admin cập nhật trạng thái thanh toán từ " + oldPaymentStatus.name() + " sang " + paymentStatus.name();
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status("PAYMENT_STATUS: " + paymentStatus.name())
                .note(historyNote)
                .build();
        
        orderStatusHistoryRepository.save(history);
        
        log.info("✅ Cập nhật trạng thái thanh toán thành công - OrderId: {}, OldStatus: {}, NewStatus: {}",
                orderId, oldPaymentStatus.name(), paymentStatus.name());
        
        return mapToOrderResponse(order);
    }
    
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId, String reason) {
        log.info("🗑️ User {} đang hủy đơn hàng: {}", userId, orderId);
        
        // 1️⃣ Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // 2️⃣ Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            log.error("❌ User {} không có quyền hủy đơn hàng: {}", userId, orderId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // 3️⃣ Kiểm tra trạng thái - Chỉ được hủy khi DANG_CHO
        if (order.getOrderStatus() != OrderStatus.DANG_CHO) {
            log.error("❌ Đơn hàng {} không thể hủy - Trạng thái hiện tại: {}", orderId, order.getOrderStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, 
                "Chỉ có thể hủy đơn hàng đang chờ xác nhận. Trạng thái hiện tại: " + order.getOrderStatus());
        }
        PaymentStatus paymentStatus = order.getPaymentStatus();
        OrderStatus orderStatus = order.getOrderStatus();
        if(paymentStatus == PaymentStatus.THAT_BAI || orderStatus == OrderStatus.DA_HUY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, 
                "Đơn hàng thanh toán thất bại " + order.getOrderStatus());
        }
        
        // 4️⃣ Sử dụng updateOrderStatusWithCashback để xử lý đầy đủ logic
        String cancellationNote = reason != null ? 
            "Khách hàng hủy đơn: " + reason : 
            "Khách hàng hủy đơn";
        
        OrderResponse cancelledOrder = updateOrderStatusWithCashback(orderId, "DA_HUY", cancellationNote);
        
        // 5️⃣ Tạo thông báo hủy đơn hàng cho admin
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            notificationDatabaseService.createNotificationForMultipleUsers(
                Arrays.asList(userId), // userId của customer (chủ sở hữu notification)
                "don_hang",
                "Đơn hàng " + order.getOrderCode() + " đã bị hủy",
                "Khách hàng " + user.getFullName() + " (" + user.getUserCode() + ") đã hủy đơn hàng. Lý do: " + 
                    (reason != null ? reason : "Không có lý do") + ". Tổng tiền: " + 
                    String.format("%,.0f", order.getTotalPayment()) + " VNĐ.",
                null,
                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
            );
            log.info("✅ Đã tạo thông báo hủy đơn hàng cho tất cả managers");
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo thông báo hủy đơn: {}", e.getMessage());
        }
        
        // 6️⃣ Xử lý hoàn tiền dựa trên phương thức thanh toán
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        PaymentMethod paymentMethod = order.getPaymentMethod();
        if (paymentMethod == PaymentMethod.COD) {
            // COD - Không cần tạo OrderRequest (khách chưa thanh toán)
            log.info("💰 COD - Đơn hàng đã hủy, không cần hoàn tiền");
            
        } else if (paymentMethod == PaymentMethod.VNPAY) {
            // VNPAY đã thanh toán - Gọi API refund, KHÔNG tạo OrderRequest
            

                
                // Thông báo admin xử lý thủ công
                // notificationDatabaseService.createNotificationForMultipleUsers(
                //     Arrays.asList(userId),
                //     "hoan_tien",
                //     "⚠️ Thiếu mã giao dịch VNPay - " + order.getOrderCode(),
                //     "Đơn hàng " + order.getOrderCode() + " không có mã giao dịch VNPay (vnp_TransactionNo). " +
                //     "Vui lòng kiểm tra và xử lý hoàn tiền thủ công.",
                //     null,
                //     com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                // );

            log.info("💳 VNPay đã thanh toán - Gọi API hoàn tiền tự động");
            
            try {
                // Gọi API VNPay refund
                boolean refundSuccess = vnpayService.refundTransaction(
                    order.getOrderCode(),
                    order.getOrderDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    order.getTotalPayment(),
                    "02", // Full refund
                    "SYSTEM_AUTO_REFUND",
                    reason != null ? reason : "Khách hàng hủy đơn hàng",
                    order.getVnpayTransactionNo() // ✅ Sử dụng vnpayTransactionNo từ VNPay
                );              
                if (refundSuccess) {
                    log.info("✅ VNPay hoàn tiền thành công - Order: {}", order.getOrderCode());
                    
                    // Thông báo 1: Cho MANAGER
                    notificationDatabaseService.createNotificationForMultipleUsers(
                        Arrays.asList(userId),
                        "hoan_tien",
                        "Hoàn tiền VNPay tự động thành công - " + order.getOrderCode(),
                        "Đơn hàng " + order.getOrderCode() + " của khách hàng " + user.getFullName() + 
                        " đã được hoàn tiền tự động qua VNPay. Số tiền: " + 
                        String.format("%,.0f", order.getTotalPayment()) + " VNĐ.",
                        null,
                        com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                    );
                    
                    // Thông báo 2: Cho CUSTOMER
                    notificationDatabaseService.createNotificationForMultipleUsers(
                        Arrays.asList(userId),
                        "hoan_tien",
                        "Hoàn tiền thành công - " + order.getOrderCode(),
                        "Đơn hàng " + order.getOrderCode() + " đã được hoàn tiền thành công. " +
                        "Số tiền " + String.format("%,.0f", order.getTotalPayment()) + " VNĐ " +
                        "sẽ được chuyển về tài khoản thanh toán của bạn trong 5-7 ngày làm việc.",
                        null,
                        com.greenconnect.greenconnect_api.enums.NotificationRecipient.CUSTOMER
                    );
                    
                    log.info("✅ Đã lưu 2 thông báo hoàn tiền (manager + customer)");
                    
                } else {
                    log.error("❌ VNPay hoàn tiền thất bại - Order: {}", order.getOrderCode());
                    
                    // Thông báo cho admin về việc thất bại
                    notificationDatabaseService.createNotificationForMultipleUsers(
                        Arrays.asList(userId),
                        "hoan_tien",
                        "⚠️ VNPay hoàn tiền tự động thất bại - " + order.getOrderCode(),
                        "Hoàn tiền VNPay tự động thất bại cho đơn hàng " + order.getOrderCode() + 
                        ". Số tiền: " + String.format("%,.0f", order.getTotalPayment()) + " VNĐ. " +
                        "Vui lòng kiểm tra và xử lý thủ công.",
                        null,
                        com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
                    );
                }
                
            } catch (Exception e) {
                log.error("❌ Lỗi khi gọi API hoàn tiền VNPay: {}", e.getMessage(), e);
            }
            }
        
        
        log.info("✅ Hủy đơn hàng thành công: {} - Lý do: {}", orderId, cancellationNote);
        
        return cancelledOrder;
    }
    
    @Override
    @Transactional
    public OrderResponse confirmOrderReceived(UUID orderId, UUID userId) {
        log.info("✅ User {} xác nhận đã nhận đơn hàng: {}", userId, orderId);
        
        // 1️⃣ Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // 2️⃣ Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            log.error("❌ User {} không có quyền xác nhận đơn hàng: {}", userId, orderId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // 3️⃣ Kiểm tra trạng thái - Chỉ được xác nhận khi DANG_GIAO
        if (order.getOrderStatus() != OrderStatus.DANG_GIAO) {
            log.error("❌ Đơn hàng {} không thể xác nhận nhận hàng - Trạng thái hiện tại: {}", 
                orderId, order.getOrderStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, 
                "Chỉ có thể xác nhận nhận hàng khi đơn đang giao. Trạng thái hiện tại: " + order.getOrderStatus());
        }
        
        // 4️⃣ Sử dụng updateOrderStatusWithCashback để xử lý đầy đủ logic
        // (cộng điểm tích lũy, sellNumber, notifications...)
        OrderResponse confirmedOrder = updateOrderStatusWithCashback(orderId, "DA_GIAO", 
            "Khách hàng xác nhận đã nhận hàng");
        
        // 5️⃣ Tạo thông báo cho admin
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            notificationDatabaseService.createNotificationForMultipleUsers(
                Arrays.asList(userId),
                "don_hang",
                "Đơn hàng " + order.getOrderCode() + " đã được giao thành công",
                "Khách hàng " + user.getFullName() + " đã xác nhận nhận được đơn hàng. Tổng tiền: " + 
                    String.format("%,.0f", order.getTotalPayment()) + " VNĐ.",
                null,
                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
            );
            log.info("✅ Đã tạo thông báo xác nhận nhận hàng cho managers");
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo thông báo: {}", e.getMessage());
        }
        
        log.info("✅ Xác nhận nhận hàng thành công: {}", orderId);
        
        return confirmedOrder;
    }
    
    @Override
    @Transactional
    public OrderResponse requestReturnOrder(UUID orderId, UUID userId, String reason) {
        log.info("🔄 User {} yêu cầu trả hàng đơn: {} - Lý do: {}", userId, orderId, reason);
        
        // 1️⃣ Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        
        // 2️⃣ Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(userId)) {
            log.error("❌ User {} không có quyền yêu cầu trả hàng đơn: {}", userId, orderId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // 3️⃣ Kiểm tra trạng thái - Chỉ được yêu cầu trả hàng khi DA_GIAO
        if (order.getOrderStatus() != OrderStatus.DA_GIAO) {
            log.error("❌ Đơn hàng {} không thể yêu cầu trả hàng - Trạng thái hiện tại: {}", 
                orderId, order.getOrderStatus());
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS, 
                "Chỉ có thể yêu cầu trả hàng khi đơn đã giao. Trạng thái hiện tại: " + order.getOrderStatus());
        }
        
        // 4️⃣ Validate reason
        if (reason == null || reason.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng nhập lý do trả hàng");
        }
        
        // 5️⃣ Cập nhật trạng thái thành YEU_CAU_TRA_HANG
        // Lưu ý: KHÔNG rollback gì cả - chờ admin duyệt TRA_HANG_THANH_CONG mới rollback
        order.setOrderStatus(OrderStatus.YEU_CAU_TRA_HANG);
        
        // 6️⃣ Tạo lịch sử trạng thái
        String returnNote = "Khách hàng yêu cầu trả hàng: " + reason;
        createOrderStatusHistory(order, OrderStatus.YEU_CAU_TRA_HANG, returnNote);
        
        order = orderRepository.save(order);
        
        // 7️⃣ Tạo thông báo cho admin
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            notificationDatabaseService.createNotificationForMultipleUsers(
                Arrays.asList(userId),
                "tra_hang",
                "Yêu cầu trả hàng đơn " + order.getOrderCode(),
                "Khách hàng " + user.getFullName() + " (" + user.getUserCode() + ") yêu cầu trả hàng. Lý do: " + 
                    reason + ". Tổng tiền: " + String.format("%,.0f", order.getTotalPayment()) + " VNĐ. Vui lòng xử lý.",
                null,
                com.greenconnect.greenconnect_api.enums.NotificationRecipient.MANAGER
            );
            log.info("✅ Đã tạo thông báo yêu cầu trả hàng cho managers");
        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo thông báo: {}", e.getMessage());
        }
        
        // 8️⃣ Gửi thông báo cho customer
        try {
            notificationDatabaseService.createNotificationForCustomer(
                userId,
                "tra_hang",
                "Yêu cầu trả hàng đã được gửi",
                "Yêu cầu trả hàng đơn " + order.getOrderCode() + " đã được gửi. Vui lòng chờ admin xử lý.",
                "/orders/" + order.getId()
            );
        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo cho customer: {}", e.getMessage());
        }
        
        log.info("✅ Yêu cầu trả hàng thành công: {} - Lý do: {}", orderId, reason);
        
        return mapToOrderResponse(order);
    }
    
    @Override
    public Page<OrderResponse> getUserOrdersWithFilter(UUID userId, String dateFilter, String statusFilter, Pageable pageable) {
        log.info("📊 User {} fetching orders - dateFilter: {}, statusFilter: {}", userId, dateFilter, statusFilter);
        log.info("📊 [DEBUG] Pageable: page={}, size={}, sort={}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = getNowVietnam();
        boolean hasDateFilter = false;
        
        // Xử lý date filter
        if (dateFilter != null && !dateFilter.equalsIgnoreCase("all")) {
            hasDateFilter = true;
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDate = getNowVietnam().toLocalDate().atStartOfDay();
                    break;
                case "this_week":
                    // ⭐ Sửa: Lấy từ thứ 2 đầu tuần (DayOfWeek.MONDAY = 1)
                    startDate = getNowVietnam()
                            .with(java.time.DayOfWeek.MONDAY)
                            .toLocalDate()
                            .atStartOfDay();
                    break;
                case "this_month":
                    // ⭐ Sửa: Lấy từ ngày đầu tiên của tháng hiện tại thay vì 30 ngày trước
                    startDate = getNowVietnam().withDayOfMonth(1).toLocalDate().atStartOfDay();
                    break;
                default:
                    hasDateFilter = false;
            }
        }
        
        Page<Order> orders;
        
        // Xử lý status filter
        if (statusFilter == null || statusFilter.equalsIgnoreCase("all")) {
            // Lấy tất cả đơn hàng của user
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.findByUserIdAndOrderDateBetweenOrderByOrderDateDesc(
                        userId, startDate, endDate, pageable);
            } else {
                orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
            }
        } else if (statusFilter.equalsIgnoreCase("DA_HUY")) {
            // Đặc biệt: DA_HUY load cả 4 trạng thái
            List<OrderStatus> cancelledStatuses = List.of(
                    OrderStatus.DA_HUY,
                    OrderStatus.YEU_CAU_TRA_HANG,
                    OrderStatus.TRA_HANG_THANH_CONG,
                    OrderStatus.TRA_HANG_THAT_BAI
            );
            
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.findByUserIdAndOrderStatusInAndOrderDateBetween(
                        userId, cancelledStatuses, startDate, endDate, pageable);
            } else {
                orders = orderRepository.findByUserIdAndOrderStatusIn(userId, cancelledStatuses, pageable);
            }
        } else {
            // Thử parse OrderStatus hoặc PaymentStatus
            String statusEnum = statusFilter.toUpperCase();
            
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(statusEnum);
                if (hasDateFilter && startDate != null) {
                    orders = orderRepository.findByUserIdAndOrderDateBetweenAndOrderStatusOrderByOrderDateDesc(
                            userId, startDate, endDate, orderStatus, pageable);
                } else {
                    orders = orderRepository.findByUserIdAndOrderStatusOrderByOrderDateDesc(
                            userId, orderStatus, pageable);
                }
            } catch (IllegalArgumentException e) {
                // Không phải OrderStatus, thử PaymentStatus
                try {
                    PaymentStatus paymentStatus = PaymentStatus.valueOf(statusEnum);
                    if (hasDateFilter && startDate != null) {
                        orders = orderRepository.findByUserIdAndOrderDateBetweenAndPaymentStatusOrderByOrderDateDesc(
                                userId, startDate, endDate, paymentStatus, pageable);
                    } else {
                        orders = orderRepository.findByUserIdAndPaymentStatusOrderByOrderDateDesc(
                                userId, paymentStatus, pageable);
                    }
                } catch (IllegalArgumentException ex) {
                    // Invalid status, return all
                    log.warn("Invalid status filter: {}, returning all orders", statusFilter);
                    if (hasDateFilter && startDate != null) {
                        orders = orderRepository.findByUserIdAndOrderDateBetweenOrderByOrderDateDesc(
                                userId, startDate, endDate, pageable);
                    } else {
                        orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId, pageable);
                    }
                }
            }
        }
        
        log.info("✅ Found {} orders for user {} (hasDateFilter={}, startDate={}, endDate={})", 
            orders.getTotalElements(), userId, hasDateFilter, startDate, endDate);
        log.info("✅ [DEBUG] Orders content size: {}, page: {}/{}", 
            orders.getContent().size(), orders.getNumber(), orders.getTotalPages());
        return orders.map(this::mapToOrderResponse);
    }
    
    @Override
    public Page<OrderResponse> getUserOrdersByReviewStatus(UUID userId, String reviewStatus, String dateFilter, Pageable pageable) {
        log.info("📝 User {} fetching orders by review status: {}, dateFilter: {}", userId, reviewStatus, dateFilter);
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = getNowVietnam();
        boolean hasDateFilter = false;
        
        // Xử lý date filter
        if (dateFilter != null && !dateFilter.equalsIgnoreCase("all")) {
            hasDateFilter = true;
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
                    break;
                case "this_week":
                    // ⭐ Sửa: Lấy từ thứ 2 đầu tuần (DayOfWeek.MONDAY = 1)
                    startDate = LocalDateTime.now()
                            .with(java.time.DayOfWeek.MONDAY)
                            .toLocalDate()
                            .atStartOfDay();
                    break;
                case "this_month":
                    // ⭐ Sửa: Lấy từ ngày đầu tiên của tháng hiện tại thay vì 30 ngày trước
                    startDate = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
                    break;
                default:
                    hasDateFilter = false;
            }
        }
        
        Page<Order> orders;
        
        // Xử lý review status
        if (reviewStatus == null || reviewStatus.equalsIgnoreCase("CHUA_DANH_GIA")) {
            // Lấy đơn hàng chưa đánh giá (DA_GIAO và chưa có review)
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.findUserOrdersWithoutReviewsByDateRange(userId, startDate, endDate, pageable);
            } else {
                orders = orderRepository.findUserOrdersWithoutReviews(userId, pageable);
            }
        } else if (reviewStatus.equalsIgnoreCase("DA_DANH_GIA")) {
            // Lấy đơn hàng đã đánh giá
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.findUserOrdersWithReviewsByDateRange(userId, startDate, endDate, pageable);
            } else {
                orders = orderRepository.findUserOrdersWithReviews(userId, pageable);
            }
        } else {
            log.warn("Invalid review status: {}, returning orders without reviews", reviewStatus);
            if (hasDateFilter && startDate != null) {
                orders = orderRepository.findUserOrdersWithoutReviewsByDateRange(userId, startDate, endDate, pageable);
            } else {
                orders = orderRepository.findUserOrdersWithoutReviews(userId, pageable);
            }
        }
        
        log.info("✅ Found {} orders with review status {} for user {}", orders.getTotalElements(), reviewStatus, userId);
        return orders.map(this::mapToOrderResponse);
    }
    
    /**
     * ⏰ Helper: Lấy LocalDateTime hiện tại với timezone Việt Nam (Asia/Ho_Chi_Minh)
     * 
     * Không cần convert ZonedDateTime nữa vì đã set TimeZone.setDefault() trong @PostConstruct
     * của GreenconnectApiApplication.java, nên LocalDateTime.now() tự động là giờ Việt Nam.
     */
    private LocalDateTime getNowVietnam() {
        return LocalDateTime.now();
    }
}
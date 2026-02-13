package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.PaymentMethod;
import com.greenconnect.greenconnect_api.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    
    private UUID id;
    private String orderCode;
    
    // ========== USER & VOUCHER ==========
    private UUID userId;
    private String userName;
    private List<VoucherInfo> vouchers; // List of vouchers used in this order
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoucherInfo {
        private UUID voucherId;
        private String voucherCode;
        private BigDecimal discountApplied;
        private LocalDateTime appliedAt;
    }
    
    // ========== DELIVERY INFORMATION ==========
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;
    private String deliveryMethodName;
    private LocalDate deliveryDate;
    private String customerNote;
    
    // ========== FINANCIAL INFORMATION ==========
    private BigDecimal totalProductAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal tax;
    private BigDecimal totalPayment;
    private BigDecimal loyaltyPoints;
    private BigDecimal rankPoints;
    
    // ========== PAYMENT & STATUS ==========
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    
    // ========== TIMESTAMPS ==========
    private LocalDateTime orderDate;
    private LocalDateTime actualDeliveryDate; // Ngày giao hàng thực tế
    
    // ========== RELATIONSHIPS ==========
    private List<OrderItemResponse> orderItems;
    private List<OrderStatusHistoryResponse> statusHistory;
    
    // ========== CALCULATED FIELDS ==========
    private Integer totalItems;
    private Integer totalQuantity;
}
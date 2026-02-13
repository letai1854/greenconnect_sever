package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.greenconnect.greenconnect_api.enums.OrderStatus;
import com.greenconnect.greenconnect_api.enums.PaymentMethod;
import com.greenconnect.greenconnect_api.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders", indexes = {
    // ✅ Critical: Lịch sử đơn hàng của user (high frequency)
    @Index(name = "idx_order_user_date", columnList = "user_id, order_date"),
    // ✅ Important: Tìm kiếm theo order code (customer support)
    @Index(name = "idx_order_code", columnList = "order_code"),
    // ✅ Essential: Admin filter theo status (business operations)
    @Index(name = "idx_order_status_date", columnList = "order_status, order_date"),
    // ✅ Useful: Tracking payment status
    @Index(name = "idx_order_payment_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_code", unique = true, length = 100)
    private String orderCode; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "delivery_address_id", insertable = false, updatable = false)
    // private RegionAddress deliveryRegionAddress;
    
    @Column(name = "recipient_name", length = 255)
    private String recipientName;
    
    @Column(name = "recipient_phone", length = 15)
    private String recipientPhone;
    
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;
    
    @Column(name = "delivery_date")
    private LocalDate deliveryDate; 
    
    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;
    
    @Column(name = "total_product_amount", precision = 15, scale = 2)
    private BigDecimal totalProductAmount;
    
    @Column(name = "shipping_fee", precision = 15, scale = 2)
    private BigDecimal shippingFee;
    
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "tax", precision = 15, scale = 2)
    private BigDecimal tax;
    
    @Column(name = "total_payment", nullable = false, precision = 15, scale = 2)

    private BigDecimal totalPayment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;
    
    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo; // Mã giao dịch VNPay để dùng cho refund
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 50)
    private OrderStatus orderStatus;
    
    @Column(name = "loyalty_points", precision = 10, scale = 2)
    private BigDecimal loyaltyPoints;
    
    @Column(name = "rank_points", precision = 10, scale = 2)
    private BigDecimal rankPoints;
        
    @CreationTimestamp
    @Column(name = "order_date")
    private LocalDateTime orderDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate; // Ngày giao hàng thực tế (khi status = DA_GIAO)
    
    // @Column(name = "delivery_address_snapshot", columnDefinition = "TEXT")
    // private String deliveryAddressSnapshot;
    
    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderStatusHistory> orderStatusHistories;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderRequest> orderRequests;
    
    /**
     * Danh sách voucher được áp dụng cho đơn hàng này
     * (Many-to-Many relationship qua bảng order_vouchers)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderVoucher> orderVouchers;
}
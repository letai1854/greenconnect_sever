package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OrderVoucher - Bảng trung gian giữa Order và Voucher (Many-to-Many)
 * 
 * <p>Một đơn hàng có thể sử dụng nhiều voucher khác nhau</p>
 * <p>Một voucher có thể được dùng trong nhiều đơn hàng khác nhau</p>
 */
@Entity
@Table(name = "order_vouchers", 
    indexes = {
        @Index(name = "idx_order_vouchers_order", columnList = "order_id"),
        @Index(name = "idx_order_vouchers_voucher", columnList = "voucher_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_voucher", columnNames = {"order_id", "voucher_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderVoucher {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;
    
    /**
     * Số tiền giảm giá thực tế đã áp dụng từ voucher này
     * (Có thể khác với voucher.discountValue nếu có maxDiscountAmount)
     */
    @Column(name = "discount_applied", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountApplied;
    
    @CreationTimestamp
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
}

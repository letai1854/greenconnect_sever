package com.greenconnect.greenconnect_api.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.greenconnect.greenconnect_api.enums.CreatedBy;
import com.greenconnect.greenconnect_api.enums.RefundStatus;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import com.greenconnect.greenconnect_api.enums.RequestType;

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
@Table(name = "order_requests", indexes = {
    @Index(name = "idx_order_request_user_status", columnList = "user_id, status"),
    @Index(name = "idx_order_request_order", columnList = "order_id"),
    // ✅ NEW: Optimization for refund tracking
    @Index(name = "idx_order_request_user_created", columnList = "user_id, created_at DESC"),
    // ✅ NEW: Admin dashboard filtering
    @Index(name = "idx_order_request_status_type", columnList = "status, request_type"),
    // ✅ NEW: Payment validation queries
    @Index(name = "idx_order_request_refund_status", columnList = "refund_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", unique = true, nullable = false)
    private Long requestId;
    
    @Column(name = "order_code", length = 100)
    private String orderCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private RequestType requestType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false, length = 20)
    private CreatedBy createdBy;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "email", length = 255)
    private String email;
    

    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private RequestStatus status = RequestStatus.DANG_XU_LY;
    
    
    // Phần hoàn tiền
    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 50)
    private RefundStatus refundStatus;
    
    @Column(name = "customer_bank_account_name", length = 255)
    private String customerBankAccountName;
    
    @Column(name = "customer_bank_account_number", length = 255)
    private String customerBankAccountNumber;
    
    @Column(name = "customer_bank_name", length = 50)
    private String customerBankName;

    
    @Column(name = "customer_bank_code", length = 255)
    private String customerBankCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private User processedByAdmin;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    
    @Column(name = "reply", columnDefinition = "TEXT")
    private String reply;

    // Lombok @Getter/@Setter are present, but provide explicit methods to ensure
    // code calling getIsStatus()/setIsStatus(...) compiles consistently.

    // Relationships
    @OneToMany(mappedBy = "orderRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RequestMedia> requestMedias;
}
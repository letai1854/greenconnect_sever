package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "history_order_status", indexes = {
    // ✅ Critical: Lịch sử trạng thái của order
    @jakarta.persistence.Index(name = "idx_order_status_history_order", columnList = "order_id"),
    // ✅ Important: Sort by time DESC
    @jakarta.persistence.Index(name = "idx_order_status_history_order_time", columnList = "order_id, updated_time"),
    // ✅ Useful: Filter by status
    @jakarta.persistence.Index(name = "idx_order_status_history_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @CreationTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}
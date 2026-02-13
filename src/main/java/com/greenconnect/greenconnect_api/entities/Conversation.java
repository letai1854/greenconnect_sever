package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * Đại diện cho một cuộc trò chuyện (container/ticket).
 * Chứa thông tin tổng quan về khách hàng, trạng thái, và nhân viên phụ trách chính.
 */
@Entity
@Table(name = "conversations", indexes = {
    @jakarta.persistence.Index(name = "idx_conversation_customer_id", columnList = "customer_id"),
    @jakarta.persistence.Index(name = "idx_conversation_assignee_id", columnList = "assignee_id"),
    @jakarta.persistence.Index(name = "idx_conversation_status", columnList = "status"),
    // ✅ NEW: Customer conversations sorted by latest
    @jakarta.persistence.Index(name = "idx_conversation_customer_updated", columnList = "customer_id, updated_at DESC"),
    // ✅ NEW: Assigned agent support tickets
    @jakarta.persistence.Index(name = "idx_conversation_assignee_status", columnList = "assignee_id, status"),
    // ✅ NEW: Status filtering with sort
    @jakarta.persistence.Index(name = "idx_conversation_status_updated", columnList = "status, updated_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    // ĐÃ THAY ĐỔI: Tên cột này thể hiện "Người được giao" hoặc "Người trả lời đầu tiên".
    // Nó có thể là NULL nếu chưa có nhân viên nào xử lý.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @Column(name = "title", length = 255)
    private String title;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status; // Ví dụ: NEW, OPEN, CLOSED
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Rất quan trọng để sắp xếp
    
    // Relationships
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @jakarta.persistence.OrderBy("createdAt DESC")
    private List<Message> messages;
}
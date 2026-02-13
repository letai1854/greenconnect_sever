package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.greenconnect.greenconnect_api.enums.Role;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction table để mapping many-to-many relationship giữa User và Role.
 * <p>Cho phép một user có nhiều roles và một role có thể được gán cho nhiều users.</p>
 * <p>Ví dụ: Một user có thể vừa là CUSTOMER vừa là SUPPLIER.</p>
 */
@Entity
@Table(name = "user_roles",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role"})
       },
       indexes = {
           @Index(name = "idx_user_roles_user_id", columnList = "user_id"),
           @Index(name = "idx_user_roles_role", columnList = "role"),
           @Index(name = "idx_user_roles_active", columnList = "active")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Reference đến User entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Role từ enum Role.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;
    
    /**
     * Trạng thái active/inactive của user-role assignment.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Ghi chú về việc gán role này (optional).
     */
    @Column(name = "notes", length = 500)
    private String notes;
    
    /**
     * Thời gian gán role.
     */
    @CreationTimestamp
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    /**
     * Thời gian hết hạn role (optional, null = permanent).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Kiểm tra role có còn valid không (chưa hết hạn và đang active).
     */
    public boolean isValid() {
        if (!active) return false;
        if (expiresAt == null) return true;
        return LocalDateTime.now().isBefore(expiresAt);
    }
    
    /**
     * Kiểm tra role có hết hạn không.
     */
    public boolean isExpired() {
        if (expiresAt == null) return false;
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favorites", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fav_user_product", columnNames = {"user_id", "product_id"})
    },
    indexes = {
        @jakarta.persistence.Index(name = "idx_favorite_user_active", columnList = "user_id, is_active"),
        @jakarta.persistence.Index(name = "idx_favorite_product", columnList = "product_id"),
        @jakarta.persistence.Index(name = "idx_favorite_created", columnList = "created_at"),
        // ✅ NEW: Join optimization (user favorites with product info)
        @jakarta.persistence.Index(name = "idx_favorite_user_product_created", columnList = "user_id, product_id, created_at"),
        // ✅ NEW: Product popularity analytics
        @jakarta.persistence.Index(name = "idx_favorite_product_active", columnList = "product_id, is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

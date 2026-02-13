package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories", indexes = {
    // ✅ Critical: Load menu categories (every page load) - Composite index tối ưu nhất
    @Index(name = "idx_category_active_order", columnList = "is_active, display_order"),
    
    // ✅ Critical: Category name uniqueness validation (create/update) - Unique constraint
    @Index(name = "idx_category_name", columnList = "name", unique = true),
    
    // ✅ Important: Admin management - filter by status
    @Index(name = "idx_category_active", columnList = "is_active"),
    
    // ✅ Important: Sorting categories in admin panel  
    @Index(name = "idx_category_display_order", columnList = "display_order"),
    
    // ✅ Important: Admin management - sort by creation time
    @Index(name = "idx_category_created_at", columnList = "created_at DESC"),
    
    // ✅ Important: Admin management - sort by update time
    @Index(name = "idx_category_updated_at", columnList = "updated_at DESC"),
    
    // ✅ Composite: Active categories sorted by order (most used query)
    @Index(name = "idx_category_active_order_created", columnList = "is_active, display_order, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url", length = 255)
    private String imageUrl;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;
}
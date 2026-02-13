package com.greenconnect.greenconnect_api.entities;

import com.greenconnect.greenconnect_api.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_media", indexes = {
    @jakarta.persistence.Index(name = "idx_supplier_media_supplier", columnList = "supplier_id"),
    @jakarta.persistence.Index(name = "idx_supplier_media_type", columnList = "media_type"),
    @jakarta.persistence.Index(name = "idx_supplier_media_display_order", columnList = "display_order"),
    // ✅ NEW: Sort supplier media by order
    @jakarta.persistence.Index(name = "idx_supplier_media_supplier_order", columnList = "supplier_id, display_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;
    
    @Column(name = "media_url", nullable = false, length = 255)
    private String mediaUrl;
    
    @Column(name = "caption", length = 255)
    private String caption;
    
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
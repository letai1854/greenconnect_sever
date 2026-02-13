package com.greenconnect.greenconnect_api.entities;

import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.MediaType;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_images", indexes = {
    // ✅ Critical: Load images theo product (product detail page)
    @Index(name = "idx_product_image_product", columnList = "product_id"),
    // ✅ Important: Main image display priority
    @Index(name = "idx_product_image_product_display", columnList = "product_id, display_order"),
    // ✅ Useful: Media type filtering
    @Index(name = "idx_product_image_media_type", columnList = "product_id, media_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;
    
    @Column(name = "media_url", nullable = false, length = 255)
    private String mediaUrl;
    
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private Boolean isMain = false;
}
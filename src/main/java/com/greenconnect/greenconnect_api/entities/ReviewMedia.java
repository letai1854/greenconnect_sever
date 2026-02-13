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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_reviews", indexes = {
    // ✅ Critical: Get media by review
    @jakarta.persistence.Index(name = "idx_review_media_review", columnList = "review_id"),
    // ✅ Useful: Filter by media type
    @jakarta.persistence.Index(name = "idx_review_media_type", columnList = "media_type"),
    // ✅ NEW: Sort media by display order
    @jakarta.persistence.Index(name = "idx_review_media_review_order", columnList = "review_id, display_order"),
    // ✅ NEW: Quick lookup for first image per review
    @jakarta.persistence.Index(name = "idx_review_media_display_order", columnList = "review_id, display_order ASC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    // Liên kết ngược lại bảng ProductReview
    @ManyToOne(fetch = FetchType.LAZY)  
    @JoinColumn(name = "review_id", nullable = false) // ⭐ SỬA TÊN CỘT: "danh_gia_id" -> "review_id"
    private ProductReview productReview;
    
    // Loại media: IMAGE hoặc VIDEO
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;
    
    // Đường dẫn tới file media
    @Column(name = "media_url", nullable = false, length = 255)
    private String mediaUrl;
    
    // Thứ tự hiển thị media (nếu có nhiều ảnh/video)
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
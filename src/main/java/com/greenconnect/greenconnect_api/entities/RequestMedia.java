package com.greenconnect.greenconnect_api.entities;

import com.greenconnect.greenconnect_api.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "request_media", indexes = {
    // ✅ Critical: Get media by order request
    @jakarta.persistence.Index(name = "idx_request_media_order_request", columnList = "order_request_id"),
    // ✅ Useful: Filter by media type
    @jakarta.persistence.Index(name = "idx_request_media_type", columnList = "media_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_request_id", nullable = false)
    private OrderRequest orderRequest;
    
    @Column(name = "media_url", nullable = false, length = 255)
    private String mediaUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;
}
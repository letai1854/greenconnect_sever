package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shipping_time_slots", indexes = {
    // ✅ Critical: Lookup by slug (service selection)
    @jakarta.persistence.Index(name = "idx_delivery_slug", columnList = "slug"),
    // ✅ Important: Filter active services
    @jakarta.persistence.Index(name = "idx_delivery_active", columnList = "is_active"),
    // ✅ Useful: Time-based queries for delivery scheduling
    @jakarta.persistence.Index(name = "idx_delivery_time_range", columnList = "start_time, end_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "slug", unique = true, length = 100)
    private String slug;
    
    @Column(name = "service_name", length = 500)
    private String serviceName;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "cutoff_time")
    private LocalTime cutoffTime;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
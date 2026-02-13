package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOptionRespone {
    private UUID id;
    private String slug; // Unique slug for URL friendly identifier
    private String serviceName; // VD: "Giao hàng đảm bảo"
    private LocalTime startTime; // VD: 09:00:00
    private LocalTime endTime; // VD: 12:00:00
    private LocalTime cutoffTime; // Phải đặt trước giờ này
    private Boolean isActive;
}

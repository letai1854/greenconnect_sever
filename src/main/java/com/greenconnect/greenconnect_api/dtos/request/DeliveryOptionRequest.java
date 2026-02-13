package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOptionRequest {

    private String slug; // Unique slug for URL friendly identifier

    @NotBlank
    private String serviceName; // VD: "Giao hàng đảm bảo"

    private LocalTime startTime; // VD: 09:00:00

    private LocalTime endTime; // VD: 12:00:00

    private LocalTime cutoffTime; // Phải đặt trước giờ này

    @Builder.Default
    private Boolean isActive = true;
}

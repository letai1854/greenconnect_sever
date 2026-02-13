package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotBlank
    private String newStatus;
    private String note;
}
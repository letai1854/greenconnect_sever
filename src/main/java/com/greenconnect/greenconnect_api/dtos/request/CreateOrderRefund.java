package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.entities.RequestMedia;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderRefund {
    @NotNull
    private UUID orderId;

    @NotNull
    private UUID userId;

    @NotNull
    private String requestType; // use string to map to RequestType enum in service

    private String reason;

    private BigDecimal refundAmount;

    private String customerBankAccountName;
    private String customerBankAccountNumber;
    private String customerBankCode;
    private String bankTransactionCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<RequestMedia> requestMediaIds; // optional media IDs (if already uploaded)
}


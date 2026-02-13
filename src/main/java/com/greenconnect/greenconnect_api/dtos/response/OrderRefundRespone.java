package com.greenconnect.greenconnect_api.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderRefundRespone {
    private UUID id;
    private UUID orderId;
    private UUID userId;
    private String requestType;
    private String reason;
    private BigDecimal refundAmount;
    private String customerBankAccountName;
    private String customerBankAccountNumber;
    private String customerBankCode;
    private String bankTransactionCode;
    private String refundStatus;
    private String requestStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UUID> requestMediaIds;
}


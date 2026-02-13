package com.greenconnect.greenconnect_api.dto;

import com.greenconnect.greenconnect_api.enums.CreatedBy;
import com.greenconnect.greenconnect_api.enums.RefundStatus;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import com.greenconnect.greenconnect_api.enums.RequestType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    
    private UUID id;
    private Long requestId;
    private String orderCode;
    private UUID orderId;
    private UUID userId;
    private RequestType requestType;
    private CreatedBy createdBy;
    private String reason;
    private String email;
    private String phoneNumber;
    private RequestStatus status;
    private BigDecimal refundAmount;
    private RefundStatus refundStatus;
    private String customerBankAccountName;
    private String customerBankAccountNumber;
    private String customerBankName;
    private String customerBankCode;
    private UUID processedByAdminId;
    private String reply;
    private List<RequestMediaDTO> requestMedias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

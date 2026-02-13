package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.entities.RequestMedia;
import com.greenconnect.greenconnect_api.entities.User;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestOrdersRefund {
    @NotNull
    private UUID orderRefundId;
    private UUID user_Id;
    private UUID orderId;
    public String requestStatus; 
    public String refundStatus;
    private String reason;
    private BigDecimal refundAmount;
    private String customerBankAccountName;
    private String customerBankAccountNumber;
    private String customerBankCode;
    private String bankTransactionCode;

    private List<RequestMedia> requestMediaIds; 
}

package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.util.List;
import com.greenconnect.greenconnect_api.entities.RequestMedia;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderRequestAdmin {
    // The admin will identify the request by path param; DTO contains fields to update
    private String requestStatus;
    private String refundStatus;
    private BigDecimal refundAmount;
    private String customerBankAccountName;
    private String customerBankAccountNumber;
    private String customerBankCode;
    private String bankTransactionCode;
    private String reason;
    private List<RequestMedia> requestMediaIds;
}

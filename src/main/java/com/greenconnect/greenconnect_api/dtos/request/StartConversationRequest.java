package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartConversationRequest {
    @NotBlank
    private String firstMessage;
}
package com.greenconnect.greenconnect_api.dtos.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID conversationId;
    private String status; // NEW, OPEN, RESOLVED, CLOSED
}

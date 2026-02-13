package com.greenconnect.greenconnect_api.dto;

import com.greenconnect.greenconnect_api.enums.MediaType;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMediaDTO {
    
    private UUID id;
    private UUID orderRequestId;
    private String mediaUrl;
    private MediaType mediaType;
}

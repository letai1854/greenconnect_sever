package com.greenconnect.greenconnect_api.dtos.response;

import com.greenconnect.greenconnect_api.enums.MediaType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMediaResponse {
    
    private UUID id;
    private MediaType mediaType;
    private String mediaUrl;
    private Integer displayOrder;
}

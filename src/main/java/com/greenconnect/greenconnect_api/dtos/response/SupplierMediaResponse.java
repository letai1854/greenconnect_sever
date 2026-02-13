package com.greenconnect.greenconnect_api.dtos.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.greenconnect.greenconnect_api.enums.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierMediaResponse {

    private UUID id;

    @JsonProperty("media_type")
    private MediaType mediaType;

    @JsonProperty("media_url")
    private String mediaUrl;
    
    private String caption;

    @JsonProperty("display_order")
    private Integer displayOrder;
}
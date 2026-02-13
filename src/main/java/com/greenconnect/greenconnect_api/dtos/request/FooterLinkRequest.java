package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.IconKey;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FooterLinkRequest {
    
    @NotNull(message = "icon_key là bắt buộc")
    private IconKey iconKey;
    
    @NotNull(message = "Giá trị là bắt buộc")
    private String value;
    
    private Boolean isActive;
}

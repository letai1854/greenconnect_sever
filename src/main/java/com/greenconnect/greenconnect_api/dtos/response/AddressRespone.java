package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRespone {
    private UUID id;
    private UUID userId;
    private String recipientName;
    private String recipientPhone;
    private String streetAddress;
    private String note;
    
    // Vietnam Address (63 provinces)
    private String provinceCode63;
    private String provinceName63;
    private String districtCode63;
    private String districtName63;
    private String wardCode63;
    private String wardName63;
    
    // Vietnam Address (34 provinces - old system)
    private String provinceCode34;
    private String provinceName34;
    private String wardCode34;
    private String wardName34;
    
    private com.greenconnect.greenconnect_api.enums.AddressType addressType;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
 

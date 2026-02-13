package com.greenconnect.greenconnect_api.dtos.response;

import com.greenconnect.greenconnect_api.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO cho địa chỉ của user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    
    private UUID id;
    private UUID userId;
    private String recipientName;
    private String recipientPhone;
    private String streetAddress;
    private String note;
    private String ward;
    private String district;
    private String city;
    private String districtNew;
    private String cityNew;
    private AddressType addressType;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

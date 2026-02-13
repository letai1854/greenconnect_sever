package com.greenconnect.greenconnect_api.mappers;

import com.greenconnect.greenconnect_api.dtos.response.AddressResponse;
import com.greenconnect.greenconnect_api.entities.Address;

/**
 * Mapper utility class for Address entity conversions.
 */
public final class AddressMapper {
    
    // Private constructor để ngăn khởi tạo instance
    private AddressMapper() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }
    
    /**
     * Chuyển đổi Address entity thành AddressResponse.
     * @param address Address entity từ database
     * @return AddressResponse để trả về client, hoặc null nếu address null
     */
    public static AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUserId())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .streetAddress(address.getStreetAddress())
                .note(address.getNote())
                .ward(address.getWardName63() != null ? address.getWardName63() : address.getWardName34())
                .district(address.getDistrictName63() != null ? address.getDistrictName63() : null)
                .city(address.getProvinceName63() != null ? address.getProvinceName63() : address.getProvinceName34())
                .districtNew(address.getDistrictName63())
                .cityNew(address.getProvinceName63())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}

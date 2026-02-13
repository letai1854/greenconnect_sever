package com.greenconnect.greenconnect_api.dtos.response;

import com.greenconnect.greenconnect_api.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRespone2 {
    private UUID id;
    private String name;
    private String description;
    private String logoUrl;
    private String address;
    private String email;
    private String phoneNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<SupplierMediaResp> supplierMedia;
    private List<SupplierCertificationResp> supplierCertifications;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierMediaResp {
        private UUID id;
        private MediaType mediaType;
        private String mediaUrl;
        private String caption;
        private Integer displayOrder;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierCertificationResp {
        private UUID certificationId;
        private String certificationName;
        private String certificationDescription;
        private String certificationLogoUrl;
        private String certificateCode;
        private LocalDate expiryDate;
    }
}

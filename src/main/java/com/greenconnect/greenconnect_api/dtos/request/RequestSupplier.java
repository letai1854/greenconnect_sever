package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.MediaType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestSupplier {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @Size(max = 255)
    private String logoUrl;

    @Size(max = 255)
    private String address;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phoneNumber;

    private Boolean isActive;

    @Valid
    private List<SupplierMediaRequest> supplierMedia;

    @Valid
    private List<SupplierCertificationRequest> supplierCertifications;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierMediaRequest {
        /** Optional id when updating an existing media; absent -> create new */
        private UUID id;

        @NotNull
        private MediaType mediaType;

        @NotBlank
        @Size(max = 255)
        private String mediaUrl;

        @Size(max = 255)
        private String caption;

        private Integer displayOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierCertificationRequest {
        /** Optional UUID of an existing Certification entity in DB. If absent, 'certification' object can be provided to create a new one. */
        private UUID certificationId;

        /** If provided, a new Certification will be created from this object. */
        private CertificationRequest certification;

        @Size(max = 255)
        private String certificateCode;

        private LocalDate expiryDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CertificationRequest {
        @NotBlank
        @Size(max = 255)
        private String name;

        @Size(max = 2000)
        private String description;

        @Size(max = 255)
        private String logoUrl;
    }
}

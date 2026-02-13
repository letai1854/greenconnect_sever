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

/**
 * Update DTO for Supplier. Same structure as RequestSupplier but allows partial updates (nullable fields).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSupplier2 {

    private String name;
    private String description;
    private String logoUrl;
    private String address;
    private String email;
    private String phoneNumber;
    private Boolean isActive;

    @Valid
    private List<RequestSupplier.SupplierMediaRequest> supplierMedia;

    @Valid
    private List<RequestSupplier.SupplierCertificationRequest> supplierCertifications;

}

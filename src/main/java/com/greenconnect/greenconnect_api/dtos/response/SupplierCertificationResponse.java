package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin về một chứng nhận mà nhà cung cấp sở hữu.
 * DTO này được "làm phẳng" (flattened) để dễ sử dụng ở phía client.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupplierCertificationResponse {

    // --- Thông tin từ Entity 'Certification' ---
    @JsonProperty("certification_id")
    private UUID certificationId;

    @JsonProperty("certification_name")
    private String certificationName;

    @JsonProperty("certification_logo_url")
    private String certificationLogoUrl;

    // --- Thông tin từ Entity 'SupplierCertification' ---
    @JsonProperty("certificate_code")
    private String certificateCode;

    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
}
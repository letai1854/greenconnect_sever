package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierResponse {

    private UUID id;

    private String name;

    private String description;

    @JsonProperty("logo_url") 
    private String logoUrl;

    private String address;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("supplier_media")
    private List<SupplierMediaResponse> supplierMedia;
    @JsonProperty("supplier_certifications")
    private List<SupplierCertificationResponse> supplierCertifications;
    @JsonProperty("product_count")
    private Long productCount;
}
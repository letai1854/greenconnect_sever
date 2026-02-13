package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "supplier_certifications", indexes = {
    @jakarta.persistence.Index(name = "idx_supplier_cert_supplier", columnList = "supplier_id"),
    @jakarta.persistence.Index(name = "idx_supplier_cert_certification", columnList = "certification_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCertification {
    
    @EmbeddedId
    private SupplierCertificationId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("supplierId")
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("certificationId")
    @JoinColumn(name = "certification_id")
    private Certification certification;
    
    @Column(name = "certificate_code", length = 255)
    private String certificateCode;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
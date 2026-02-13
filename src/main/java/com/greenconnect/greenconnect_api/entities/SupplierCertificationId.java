package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierCertificationId implements Serializable {
    
    private UUID supplierId;
    private UUID certificationId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupplierCertificationId that = (SupplierCertificationId) o;
        return Objects.equals(supplierId, that.supplierId) && 
               Objects.equals(certificationId, that.certificationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(supplierId, certificationId);
    }
}
package com.greenconnect.greenconnect_api.services;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierRequest;
import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
public interface SupplierService {
    SupplierResponse createSupplier(SupplierRequest request);
    SupplierResponse UpdateSupplier(UUID id,SupplierUpdateRequest request);
    List<SupplierResponse> getAllSuppliers();
}
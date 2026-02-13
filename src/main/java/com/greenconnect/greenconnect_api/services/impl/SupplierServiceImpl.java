// package com.greenconnect.greenconnect_api.services.impl;

// import java.util.List;
// import java.util.UUID;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierRequest;
// import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierUpdateRequest;
// import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
// import com.greenconnect.greenconnect_api.entities.Supplier;
// import com.greenconnect.greenconnect_api.exceptions.AppException;
// import com.greenconnect.greenconnect_api.exceptions.BusinessException;
// import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
// import com.greenconnect.greenconnect_api.mappers.SupplierMapper;
// import com.greenconnect.greenconnect_api.repositories.SupplierRepository;
// import com.greenconnect.greenconnect_api.services.SupplierService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class SupplierServiceImpl implements SupplierService {
    
//     @Autowired
//     private SupplierRepository supplierRepository;

//     @Override
//     @Transactional
//     public SupplierResponse createSupplier(SupplierRequest request) {
//         log.info("Bắt đầu tạo nhà cung cấp với tên: {}", request.getName());
        
//         // Kiểm tra tên nhà cung cấp đã tồn tại
//         if (supplierRepository.existsByName(request.getName())) {
//             log.warn("Tên nhà cung cấp đã tồn tại: {}", request.getName());
//             throw new BusinessException(ErrorCode.SUPPLIER_NAME_ALREADY_EXISTS);
//         }
        
//         // Kiểm tra email đã tồn tại
//         if (supplierRepository.existsByEmail(request.getEmail())) {
//             log.warn("Email nhà cung cấp đã tồn tại: {}", request.getEmail());
//             throw new BusinessException(ErrorCode.SUPPLIER_EMAIL_ALREADY_EXISTS);
//         }
        
//         // Chuyển đổi request thành entity bằng mapper
//         Supplier supplier = SupplierMapper.toEntity(request);
        
//         // Lưu vào database
//         Supplier savedSupplier = supplierRepository.save(supplier);
//         log.info("Đã tạo thành công nhà cung cấp với ID: {}", savedSupplier.getId());
        
//         // Chuyển đổi thành response bằng mapper
//         return SupplierMapper.toResponse(savedSupplier);
//     }

//     @Override
//     public SupplierResponse UpdateSupplier(UUID id, SupplierUpdateRequest request) {
//        if(!supplierRepository.existsById(id)) {
//            log.warn("ID Nhà cung cấp không tồn tại: {}", request.getName());
//            throw new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND);
//        }
//        Supplier supplierFind = supplierRepository.findById(id).orElseThrow(()->new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
//        Supplier supplier =   SupplierMapper.updateEntity(supplierFind,request); 
//        supplierRepository.save(supplier);
//        return SupplierMapper.toResponse(supplier);
//     }

//     @Override
//     public List<SupplierResponse> getAllSuppliers() {
//         List<Supplier> suppliers = supplierRepository.findAll();
//         return SupplierMapper.toResponseList(suppliers);
//     }



// }
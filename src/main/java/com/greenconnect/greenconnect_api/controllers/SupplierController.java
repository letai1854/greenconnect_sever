package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierRequest;
import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.services.SupplierService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/suppliers")
public class SupplierController {
    
    // @Autowired
    // private SupplierService supplierService;
    
    // @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY  
    // private ElasticsearchSyncService elasticsearchSyncService;
    
    // /**
    //  * Tạo nhà cung cấp mới
    //  * 
    //  * @param request SupplierRequest chứa thông tin nhà cung cấp
    //  * @return ApiResponse với SupplierResponse và HTTP 201 CREATED
    //  */
    // @PostMapping("/create")
    // public ResponseEntity<ApiResponse<SupplierResponse>> CreateSupplier(
    //         @Valid @RequestBody SupplierRequest request) {
        
    //     log.info("REST API: Nhận yêu cầu tạo nhà cung cấp - {}", request.getName());
        
    //     SupplierResponse response = supplierService.createSupplier(request);
        
    //     // 🔄 SYNC VÀO ELASTICSEARCH (sync tất cả products của supplier này)
    //     if (elasticsearchSyncService != null) {
    //         try {
    //             elasticsearchSyncService.syncSupplier(response.getId());
    //             log.info("✅ Đã sync supplier {} vào Elasticsearch", response.getId());
    //         } catch (Exception e) {
    //             log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
    //         }
    //     } else {
    //         log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
    //     }
        
    //     ApiResponse<SupplierResponse> apiResponse = ResponseUtil.success(
    //         response, 
    //         "Tạo nhà cung cấp thành công"
    //     );
        
    //     return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    // }
    // @PutMapping("update/{id}")
    // public ResponseEntity<ApiResponse<SupplierResponse>>  UpdateSupplier(@PathVariable String id, @RequestBody SupplierUpdateRequest request) {
    //     log.info("REST API: Nhận yêu cầu update nhà cung cấp - {}", request.getName());
    //     UUID supplierId = UUID.fromString(id);
    //     SupplierResponse response = supplierService.UpdateSupplier(supplierId, request);
        
    //     // 🔄 SYNC VÀO ELASTICSEARCH (sync tất cả products của supplier này)
    //     if (elasticsearchSyncService != null) {
    //         try {
    //             elasticsearchSyncService.syncSupplier(supplierId);
    //             log.info("✅ Đã sync supplier {} vào Elasticsearch", supplierId);
    //         } catch (Exception e) {
    //             log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
    //         }
    //     } else {
    //         log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
    //     }
        
    //     ApiResponse<SupplierResponse> apiResponse = ResponseUtil.success(
    //         response, 
    //         "Cập nhật nhà cung cấp thành công"
    //     );
    //     return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    // }
    //     //     @PutMapping("/profile")
    // // @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    // // public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(
    // //         @Valid @RequestBody UserUpdateRequest request) {
    // //     UUID currentUserId = SecurityUtils.getCurrentUserId();
    // //     UserResponse user = userService.updateUser(currentUserId, request);
    // //     return ResponseUtil.success(user, "Cập nhật profile thành công");
    // // }
    // @GetMapping("/All")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers() {
    //     log.info("REST API: Nhận yêu cầu lấy tất cả nhà cung cấp");
    //     List<SupplierResponse> response = supplierService.getAllSuppliers();
    //     ApiResponse<List<SupplierResponse>> apiResponse = ResponseUtil.success(
    //         response, 
    //         "Lấy tất cả nhà cung cấp thành công"
    //     );
    //     return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    // }

}
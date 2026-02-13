package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.RequestSupplier;
import com.greenconnect.greenconnect_api.dtos.request.UpdateSupplier2;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.services.SupplierService2;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController2 {

	private final SupplierService2 supplierService;
    @Autowired(required = false) 
    private ElasticsearchSyncService elasticsearchSyncService;

	@PostMapping("/create")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(@Valid @RequestBody RequestSupplier request) {
		SupplierResponse resp = supplierService.createSupplier(request);
		 if (elasticsearchSyncService != null) {
            try {
                java.util.UUID supplierId = resp.getId();
                elasticsearchSyncService.syncSupplier(supplierId);
                log.info("✅ Đã sync supplier {} vào Elasticsearch", supplierId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        return ResponseEntity.ok(ResponseUtil.success(resp, "Supplier created"));
	}

	@GetMapping("/{id}/fullinfo")
	public ResponseEntity<ApiResponse<com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2>> getSupplierFull(@PathVariable("id") java.util.UUID id) {
		com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2 resp = supplierService.getSupplierFull(id);
		return ResponseEntity.ok(ResponseUtil.success(resp, "Supplier full data"));
	}

	@PutMapping("/{id}/update")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(@PathVariable("id") java.util.UUID id, @Valid @RequestBody UpdateSupplier2 request) {
        SupplierResponse resp = supplierService.updateSupplier(id, request);
         if (elasticsearchSyncService != null) {
            try {
                java.util.UUID supplierId = resp.getId();
                elasticsearchSyncService.syncSupplier(supplierId);
                log.info("✅ Đã sync supplier {} vào Elasticsearch", supplierId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        return ResponseEntity.ok(ResponseUtil.success(resp, "Supplier updated"));
    }

	@PutMapping("/{id}/active")
	@PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplierActive(@PathVariable("id") java.util.UUID id, @RequestParam("active") Boolean active) {
		SupplierResponse resp = supplierService.updateSupplierIsActive(id, active);
		 if (elasticsearchSyncService != null) {
            try {
                java.util.UUID supplierId = resp.getId();
                elasticsearchSyncService.syncSupplier(supplierId);
                log.info("✅ Đã sync supplier {} vào Elasticsearch", supplierId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        return ResponseEntity.ok(ResponseUtil.success(resp, "Supplier active flag updated"));
	}

    // @GetMapping("/admin/all") @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")

    // @GetMapping("/admin/{categoryId}")     @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")

    
    //@GetMapping("/active")

    //@GetMapping("/admin/inactive") 

    // @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")

    /**
     * Lấy danh sách nhà cung cấp đang hoạt động sắp xếp theo thời gian tạo giảm dần - Public
     */
    @GetMapping("/active/latest")
    public ApiResponse<List<SupplierResponse>> getActiveSuppliersbyCreatedAtDesc() {
        log.info("Lấy danh sách nhà cung cấp đang hoạt động sắp xếp theo thời gian tạo giảm dần");
        List<SupplierResponse> response = supplierService.getActiveSuppliersbyCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    }
    
    /**
     * Lấy danh sách nhà cung cấp không hoạt động sắp xếp theo thời gian tạo giảm dần - Admin
     */
    @GetMapping("/admin/inactive/latest")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<SupplierResponse>> getInactiveSuppliersbyCreatedAtDesc() {
        log.info("Admin lấy danh sách nhà cung cấp không hoạt động sắp xếp theo thời gian tạo giảm dần");
        List<SupplierResponse> response = supplierService.getInactiveSuppliersByCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp không hoạt động thành công");
    }
    
    /**
     * Lấy tất cả nhà cung cấp sắp xếp theo thời gian tạo giảm dần - Admin
     */
    @GetMapping("/admin/all/latest")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<SupplierResponse>> getAllSuppliersbyCreatedAtDesc() {
        log.info("Admin lấy tất cả nhà cung cấp sắp xếp theo thời gian tạo giảm dần");
        List<SupplierResponse> response = supplierService.getAllSuppliersByCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    }
    
    /**
     * Lấy danh sách nhà cung cấp đang hoạt động với phân trang sắp xếp theo thời gian tạo - Public
     */
    @GetMapping("/active/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<SupplierResponse>> getActiveSuppliersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy danh sách nhà cung cấp đang hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<SupplierResponse> response = supplierService.getActiveSuppliersPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    }
    
    /**
     * Lấy danh sách nhà cung cấp không hoạt động với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/inactive/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<SupplierResponse>> getInactiveSuppliersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách nhà cung cấp không hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<SupplierResponse> response = supplierService.getInactiveSuppliersPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp không hoạt động thành công");
    }
    
    /**
     * Lấy tất cả nhà cung cấp với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/all/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<SupplierResponse>> getAllSuppliersPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả nhà cung cấp với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<SupplierResponse> response = supplierService.getAllSuppliersPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    }
    
    /**
     * Lấy tất cả nhà cung cấp với phân trang sắp xếp theo thời gian tạo - Public (không cần xác thực)
     * Chỉ lấy các nhà cung cấp đang hoạt động (isActive = true)
     */
    @GetMapping("/public/all/latest/page")
    public ApiResponse<Page<SupplierResponse>> getAllSuppliersPaginatedByCreatedAtPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Public lấy nhà cung cấp ACTIVE với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<SupplierResponse> response = supplierService.getActiveSuppliersPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    }
    
    /**
     * Tìm kiếm nhà cung cấp với phân trang - Hỗ trợ tiếng Việt có dấu
     * @param keyword Từ khóa tìm kiếm (tiếng Việt có dấu)
     * @param tab Trạng thái: active, inactive, all
     * @param page Số trang (mặc định 0)
     * @param size Số lượng/trang (mặc định 20)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<SupplierResponse>> searchSuppliers(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔍 Tìm kiếm nhà cung cấp - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        // Validate tab
        if (!tab.matches("^(active|inactive|all)$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        Page<SupplierResponse> response = supplierService.searchSuppliers(keyword, tab, page, size);
        return ResponseUtil.success(response, "Tìm kiếm nhà cung cấp thành công - Tìm thấy " + response.getTotalElements() + " kết quả");
    }
    
    /**
     * Tìm kiếm nhà cung cấp với phân trang - Sử dụng Request Body (Alternative)
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<SupplierResponse>> searchSuppliersWithBody(
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.SearchSupplierRequest request) {
        log.info("🔍 Tìm kiếm nhà cung cấp (POST) - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", 
                request.getKeyword(), request.getTab(), request.getPage(), request.getSize());
        
        Page<SupplierResponse> response = supplierService.searchSuppliers(
            request.getKeyword(), 
            request.getTab(), 
            request.getPage(), 
            request.getSize()
        );
        return ResponseUtil.success(response, "Tìm kiếm nhà cung cấp thành công - Tìm thấy " + response.getTotalElements() + " kết quả");
    }
    
    /**
     * Lấy tất cả nhà cung cấp - Public (chỉ active)
     */
    // @GetMapping("/GetAll")
    // public ApiResponse<List<SupplierResponse>> getPublicSuppliers() {
    //     log.info("Lấy danh sách nhà cung cấp (public)");
    //     List<SupplierResponse> response = supplierService.getActiveSuppliers();
    //     return ResponseUtil.success(response, "Lấy danh sách nhà cung cấp thành công");
    // }
    
    /**
     * Lấy nhà cung cấp theo ID - Public (chỉ nếu active)
     */
    @GetMapping("/{supplierId}")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2> getSupplierById(@PathVariable UUID supplierId) {
        log.info("Lấy nhà cung cấp theo ID: {}", supplierId);
        com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2 response = supplierService.getSupplierFull(supplierId);
        
        // Kiểm tra nếu nhà cung cấp không active thì không cho truy cập public
        if (!response.getIsActive()) {
            throw new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        
        return ResponseUtil.success(response, "Lấy thông tin nhà cung cấp thành công");
    }
}

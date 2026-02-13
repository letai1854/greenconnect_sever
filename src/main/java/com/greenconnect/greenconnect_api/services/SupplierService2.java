package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.RequestSupplier;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface SupplierService2 {
	SupplierResponse createSupplier(RequestSupplier request);
	com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2 getSupplierFull(UUID id);
	com.greenconnect.greenconnect_api.dtos.response.SupplierResponse updateSupplier(java.util.UUID id, com.greenconnect.greenconnect_api.dtos.request.UpdateSupplier2 request);

	/** Update only isActive flag for a supplier */
	com.greenconnect.greenconnect_api.dtos.response.SupplierResponse updateSupplierIsActive(java.util.UUID id, Boolean isActive);
	
	/**
	 * Lấy danh sách nhà cung cấp active sắp xếp theo thời gian tạo giảm dần
	 */
	List<SupplierResponse> getActiveSuppliersbyCreatedAtDesc();
	
	/**
	 * Lấy danh sách nhà cung cấp inactive sắp xếp theo thời gian tạo giảm dần
	 */
	List<SupplierResponse> getInactiveSuppliersByCreatedAtDesc();
	
	/**
	 * Lấy tất cả nhà cung cấp sắp xếp theo thời gian tạo giảm dần
	 */
	List<SupplierResponse> getAllSuppliersByCreatedAtDesc();
	
	/**
	 * Lấy danh sách nhà cung cấp active với phân trang sắp xếp theo thời gian tạo
	 */
	Page<SupplierResponse> getActiveSuppliersPaginatedByCreatedAt(int page, int size);
	
	/**
	 * Lấy danh sách nhà cung cấp inactive với phân trang sắp xếp theo thời gian tạo
	 */
	Page<SupplierResponse> getInactiveSuppliersPaginatedByCreatedAt(int page, int size);
	
	/**
	 * Lấy tất cả nhà cung cấp với phân trang sắp xếp theo thời gian tạo
	 */
	Page<SupplierResponse> getAllSuppliersPaginatedByCreatedAt(int page, int size);
	
	/**
	 * Tìm kiếm nhà cung cấp với phân trang theo từ khóa và tab (active/inactive/all)
	 */
	Page<SupplierResponse> searchSuppliers(String keyword, String tab, int page, int size);
}

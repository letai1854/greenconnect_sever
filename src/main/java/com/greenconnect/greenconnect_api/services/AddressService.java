package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.AddressRequest;
import com.greenconnect.greenconnect_api.dtos.response.AddressRespone;

/**
 * Service interface cho quản lý địa chỉ của user.
 */
public interface AddressService {
	
	/**
	 * Lấy danh sách tất cả địa chỉ của user.
	 * 
	 * @param userId ID của user
	 * @return Danh sách địa chỉ của user
	 */
	List<AddressRespone> getUserAddresses(UUID userId);
	
	/**
	 * Tạo địa chỉ mới cho user.
	 * 
	 * @param request Thông tin địa chỉ cần tạo
	 * @return Địa chỉ vừa tạo
	 */
	AddressRespone createAddress(AddressRequest request);
	
	/**
	 * Cập nhật thông tin địa chỉ.
	 * 
	 * @param id ID của địa chỉ cần update
	 * @param request Thông tin địa chỉ mới
	 * @return Địa chỉ sau khi update
	 */
	AddressRespone updateAddress(UUID id, AddressRequest request);
	
	/**
	 * Xóa địa chỉ.
	 * 
	 * @param id ID của địa chỉ cần xóa
	 */
	void deleteAddress(UUID id);
}

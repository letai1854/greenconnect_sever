package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.AddressRequest;
import com.greenconnect.greenconnect_api.dtos.response.AddressRespone;
import com.greenconnect.greenconnect_api.entities.Address;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.repositories.AddressRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.AddressService;
import com.greenconnect.greenconnect_api.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Thêm import này để đảm bảo tính toàn vẹn

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdressServiceImpl implements AddressService {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public List<AddressRespone> getUserAddresses(UUID userId) {
		UUID currentUserId = SecurityUtils.getCurrentUserId();
		
		// Kiểm tra quyền: chỉ cho phép user xem địa chỉ của chính mình hoặc admin xem tất cả
		if (!userId.equals(currentUserId) && !SecurityUtils.isAdmin()) {
			throw new com.greenconnect.greenconnect_api.exceptions.AppException(
					com.greenconnect.greenconnect_api.exceptions.ErrorCode.FORBIDDEN,
					"Không có quyền xem địa chỉ của user khác");
		}
		
		// Kiểm tra user có tồn tại không
		userRepository.findById(userId)
				.orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
					com.greenconnect.greenconnect_api.exceptions.ErrorCode.USER_NOT_FOUND,
					"User not found: " + userId));
		
		List<Address> addresses = addressRepository.findByUser_Id(userId);
		
		log.info("Found {} addresses for user {}", addresses.size(), userId);
		
		return addresses.stream()
				.map(address -> AddressRespone.builder()
						.id(address.getId())
						.userId(address.getUserId())
						.recipientName(address.getRecipientName())
						.recipientPhone(address.getRecipientPhone())
						.streetAddress(address.getStreetAddress())
						.note(address.getNote())
						// Vietnam Address (63 provinces)
						.provinceCode63(address.getProvinceCode63())
						.provinceName63(address.getProvinceName63())
						.districtCode63(address.getDistrictCode63())
						.districtName63(address.getDistrictName63())
						.wardCode63(address.getWardCode63())
						.wardName63(address.getWardName63())
						// Vietnam Address (34 provinces - optional)
						.provinceCode34(address.getProvinceCode34())
						.provinceName34(address.getProvinceName34())
						.wardCode34(address.getWardCode34())
						.wardName34(address.getWardName34())
						.addressType(address.getAddressType())
						.isDefault(address.getIsDefault())
						.createdAt(address.getCreatedAt())
						.updatedAt(address.getUpdatedAt())
						.build())
				.toList();
	}

	@Override
	@Transactional // Thêm Transactional để đảm bảo tất cả các thao tác DB (update, insert) thành công hoặc thất bại cùng nhau
	public AddressRespone createAddress(AddressRequest request) {
		UUID currentUserId = SecurityUtils.getCurrentUserId();

		// Logic xác định targetUserId đã rất tốt, không cần thay đổi
		UUID targetUserId = request.getUserId() == null ? currentUserId : request.getUserId();

		// Logic kiểm tra quyền admin cũng đã tốt
		if (!targetUserId.equals(currentUserId) && !SecurityUtils.isAdmin()) {
			throw new com.greenconnect.greenconnect_api.exceptions.AppException(
					com.greenconnect.greenconnect_api.exceptions.ErrorCode.FORBIDDEN,
					"Không có quyền tạo địa chỉ cho user khác");
		}

		User user = userRepository.findById(targetUserId)
				.orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
					com.greenconnect.greenconnect_api.exceptions.ErrorCode.USER_NOT_FOUND,
					"User not found: " + targetUserId));

		// Nếu isDefault = true, bỏ default của các địa chỉ khác
		if (Boolean.TRUE.equals(request.getIsDefault())) {
			// SỬA LỖI: Gọi phương thức repository đã được đổi tên
			List<Address> existingAddresses = addressRepository.findByUser_Id(targetUserId);
			existingAddresses.stream()
				.filter(Address::getIsDefault) // Lọc chỉ những địa chỉ đang là default
				.forEach(a -> a.setIsDefault(false));
			// saveAll không cần thiết nếu có @Transactional, JPA sẽ tự động flush thay đổi
			// addressRepository.saveAll(existingAddresses); 
		}

		Address entity = Address.builder()
				.user(user)
				.recipientName(request.getRecipientName())
				.recipientPhone(request.getRecipientPhone())
				.streetAddress(request.getStreetAddress())
				.note(request.getNote())
				// Vietnam Address (63 provinces)
				.provinceCode63(request.getProvinceCode63())
				.provinceName63(request.getProvinceName63())
				.districtCode63(request.getDistrictCode63())
				.districtName63(request.getDistrictName63())
				.wardCode63(request.getWardCode63())
				.wardName63(request.getWardName63())
				// Vietnam Address (34 provinces - optional)
				.provinceCode34(request.getProvinceCode34())
				.provinceName34(request.getProvinceName34())
				.wardCode34(request.getWardCode34())
				.wardName34(request.getWardName34())
				.addressType(request.getAddressType())
				.isDefault(request.getIsDefault() != null && request.getIsDefault())
				.build();

		Address saved = addressRepository.save(entity);

		AddressRespone resp = AddressRespone.builder()
				.id(saved.getId())
				.userId(user.getId())
				.recipientName(saved.getRecipientName())
				.recipientPhone(saved.getRecipientPhone())
				.streetAddress(saved.getStreetAddress())
				.note(saved.getNote())
				// Vietnam Address (63 provinces)
				.provinceCode63(saved.getProvinceCode63())
				.provinceName63(saved.getProvinceName63())
				.districtCode63(saved.getDistrictCode63())
				.districtName63(saved.getDistrictName63())
				.wardCode63(saved.getWardCode63())
				.wardName63(saved.getWardName63())
				// Vietnam Address (34 provinces - optional)
				.provinceCode34(saved.getProvinceCode34())
				.provinceName34(saved.getProvinceName34())
				.wardCode34(saved.getWardCode34())
				.wardName34(saved.getWardName34())
				.addressType(saved.getAddressType())
				.isDefault(saved.getIsDefault())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.build();

		log.info("Address created for user {}: {}", targetUserId, resp.getId());
		return resp;
	}

	@Override
	@Transactional
	public AddressRespone updateAddress(UUID id, AddressRequest request) {
		UUID currentUserId = SecurityUtils.getCurrentUserId();

		Address existing = addressRepository.findById(id)
				.orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
						com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
						"Address not found: " + id));

		UUID ownerId = existing.getUserId();
		if (!ownerId.equals(currentUserId) && !SecurityUtils.isAdmin()) {
			throw new com.greenconnect.greenconnect_api.exceptions.AppException(
						com.greenconnect.greenconnect_api.exceptions.ErrorCode.FORBIDDEN,
						"Không có quyền cập nhật địa chỉ này");
		}

		// Update fields only when provided
		if (request.getRecipientName() != null) existing.setRecipientName(request.getRecipientName());
		if (request.getRecipientPhone() != null) existing.setRecipientPhone(request.getRecipientPhone());
		if (request.getStreetAddress() != null) existing.setStreetAddress(request.getStreetAddress());
		if (request.getNote() != null) existing.setNote(request.getNote());
		
		// Vietnam Address (63 provinces)
		if (request.getProvinceCode63() != null) existing.setProvinceCode63(request.getProvinceCode63());
		if (request.getProvinceName63() != null) existing.setProvinceName63(request.getProvinceName63());
		if (request.getDistrictCode63() != null) existing.setDistrictCode63(request.getDistrictCode63());
		if (request.getDistrictName63() != null) existing.setDistrictName63(request.getDistrictName63());
		if (request.getWardCode63() != null) existing.setWardCode63(request.getWardCode63());
		if (request.getWardName63() != null) existing.setWardName63(request.getWardName63());
		
		// Vietnam Address (34 provinces - optional)
		if (request.getProvinceCode34() != null) existing.setProvinceCode34(request.getProvinceCode34());
		if (request.getProvinceName34() != null) existing.setProvinceName34(request.getProvinceName34());
		if (request.getWardCode34() != null) existing.setWardCode34(request.getWardCode34());
		if (request.getWardName34() != null) existing.setWardName34(request.getWardName34());
		
		if (request.getAddressType() != null) existing.setAddressType(request.getAddressType());

		// handle default flag
		if (Boolean.TRUE.equals(request.getIsDefault())) {
			List<Address> defaults = addressRepository.findByUser_Id(ownerId);
			for (Address a : defaults) {
				if (!a.getId().equals(existing.getId()) && Boolean.TRUE.equals(a.getIsDefault())) {
					a.setIsDefault(false);
				}
			}
			existing.setIsDefault(true);
		} else if (request.getIsDefault() != null) {
			existing.setIsDefault(request.getIsDefault());
		}

		Address saved = addressRepository.save(existing);

		AddressRespone resp = AddressRespone.builder()
				.id(saved.getId())
				.userId(saved.getUserId())
				.recipientName(saved.getRecipientName())
				.recipientPhone(saved.getRecipientPhone())
				.streetAddress(saved.getStreetAddress())
				.note(saved.getNote())
				// Vietnam Address (63 provinces)
				.provinceCode63(saved.getProvinceCode63())
				.provinceName63(saved.getProvinceName63())
				.districtCode63(saved.getDistrictCode63())
				.districtName63(saved.getDistrictName63())
				.wardCode63(saved.getWardCode63())
				.wardName63(saved.getWardName63())
				// Vietnam Address (34 provinces - optional)
				.provinceCode34(saved.getProvinceCode34())
				.provinceName34(saved.getProvinceName34())
				.wardCode34(saved.getWardCode34())
				.wardName34(saved.getWardName34())
				.addressType(saved.getAddressType())
				.isDefault(saved.getIsDefault())
				.createdAt(saved.getCreatedAt())
				.updatedAt(saved.getUpdatedAt())
				.build();

		log.info("Address updated id={} by user {}", id, currentUserId);
		return resp;
	}

	@Override
	@Transactional
	public void deleteAddress(UUID id) {
		UUID currentUserId = SecurityUtils.getCurrentUserId();

		Address existing = addressRepository.findById(id)
				.orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
						com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
						"Address not found: " + id));

		UUID ownerId = existing.getUserId();
		if (!ownerId.equals(currentUserId) && !SecurityUtils.isAdmin()) {
			throw new com.greenconnect.greenconnect_api.exceptions.AppException(
						com.greenconnect.greenconnect_api.exceptions.ErrorCode.FORBIDDEN,
						"Không có quyền xóa địa chỉ này");
		}

		addressRepository.delete(existing);
		log.info("Address deleted id={} by user {}", id, currentUserId);
	}
}
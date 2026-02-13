package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.AddressRequest;
import com.greenconnect.greenconnect_api.dtos.response.AddressRespone;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
// import com.greenconnect.greenconnect_api.security.CustomUserPrincipal; // Không cần nữa
import com.greenconnect.greenconnect_api.services.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.context.SecurityContextHolder; // Không cần nữa
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import static lombok.AccessLevel.PRIVATE;

// import java.util.UUID; // Không cần nữa

@Slf4j
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class AddressController {

	AddressService addressService;

	@PostMapping("/create")
	public ResponseEntity<ApiResponse<AddressRespone>> create(@Valid @RequestBody AddressRequest request) {
		// SỬA LỖI: Xóa bỏ logic thừa.
		// Tầng service đã được thiết kế để xử lý trường hợp request.getUserId() là null
		// (sẽ tự động lấy người dùng đang đăng nhập).
		// Việc này giúp controller gọn gàng và logic nghiệp vụ được tập trung ở service.
		AddressRespone resp = addressService.createAddress(request);
		return ResponseEntity.ok(ResponseUtil.success(resp, "Tạo địa chỉ giao hàng thành công"));
	}
    
	@PutMapping("/update/{id}")
	public ResponseEntity<ApiResponse<AddressRespone>> update(@PathVariable("id") java.util.UUID id,
															  @Valid @RequestBody AddressRequest request) {
		AddressRespone resp = addressService.updateAddress(id, request);
		return ResponseEntity.ok(ResponseUtil.success(resp, "Cập nhật địa chỉ thành công"));
	}
    
	@org.springframework.web.bind.annotation.DeleteMapping("/delete/{id}")
	public ResponseEntity<ApiResponse<Object>> delete(@PathVariable("id") java.util.UUID id) {
		addressService.deleteAddress(id);
		return ResponseEntity.ok(ResponseUtil.success(null, "Xóa địa chỉ thành công"));
	}
    
}
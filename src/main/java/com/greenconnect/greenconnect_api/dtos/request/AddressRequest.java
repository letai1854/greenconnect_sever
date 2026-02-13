package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho việc tạo mới hoặc cập nhật địa chỉ.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {
	
	@NotBlank(message = "Tên người nhận không được để trống")
	@Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
	private String recipientName;

	@NotBlank(message = "Số điện thoại người nhận không được để trống")
	@Size(min = 7, max = 20, message = "Số điện thoại phải từ 7-20 ký tự")
	private String recipientPhone;

	@NotBlank(message = "Địa chỉ đường không được để trống")
	@Size(max = 255, message = "Địa chỉ đường không được vượt quá 255 ký tự")
	private String streetAddress;
	
	@Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
	private String note;

	// ========== Vietnam Address (63 provinces) - All optional ==========
	
	@Size(max = 10, message = "Mã tỉnh không được vượt quá 10 ký tự")
	private String provinceCode63;
	
	@Size(max = 100, message = "Tên tỉnh không được vượt quá 100 ký tự")
	private String provinceName63;
	
	@Size(max = 10, message = "Mã quận/huyện không được vượt quá 10 ký tự")
	private String districtCode63;
	
	@Size(max = 100, message = "Tên quận/huyện không được vượt quá 100 ký tự")
	private String districtName63;
	
	@Size(max = 10, message = "Mã phường/xã không được vượt quá 10 ký tự")
	private String wardCode63;
	
	@Size(max = 100, message = "Tên phường/xã không được vượt quá 100 ký tự")
	private String wardName63;
	
	// ========== Vietnam Address (34 provinces - old system, optional) ==========
	
	@Size(max = 10, message = "Mã tỉnh (34) không được vượt quá 10 ký tự")
	private String provinceCode34;
	
	@Size(max = 100, message = "Tên tỉnh (34) không được vượt quá 100 ký tự")
	private String provinceName34;
	
	@Size(max = 10, message = "Mã phường/xã (34) không được vượt quá 10 ký tự")
	private String wardCode34;
	
	@Size(max = 100, message = "Tên phường/xã (34) không được vượt quá 100 ký tự")
	private String wardName34;
	
	@NotNull(message = "Loại địa chỉ không được để trống")
	private AddressType addressType;

	/**
	 * Nếu true => đặt địa chỉ này là mặc định cho user (cần clear các default khác)
	 */
	@Builder.Default
	private Boolean isDefault = false;
	
	/**
	 * Optional: If provided, creates the address for this userId. If omitted,
	 * the address will be created for the current authenticated user.
	 */
	private java.util.UUID userId;
}
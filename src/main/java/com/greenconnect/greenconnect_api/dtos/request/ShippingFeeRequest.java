package com.greenconnect.greenconnect_api.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request để tính phí vận chuyển qua GHTK API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingFeeRequest {
    
    @NotBlank(message = "Tỉnh/thành phố gửi hàng không được để trống")
    @JsonProperty("pick_province")
    private String pickProvince;
    
    @NotBlank(message = "Quận/huyện gửi hàng không được để trống")
    @JsonProperty("pick_district")
    private String pickDistrict;
    
    @JsonProperty("pick_ward")
    private String pickWard; // Optional
    
    @NotBlank(message = "Tỉnh/thành phố nhận hàng không được để trống")
    @JsonProperty("province")
    private String province;
    
    @NotBlank(message = "Quận/huyện nhận hàng không được để trống")
    @JsonProperty("district")
    private String district;
    
    @JsonProperty("ward")
    private String ward; // Optional
    
    @NotNull(message = "Khối lượng không được để trống")
    @Min(value = 1, message = "Khối lượng phải lớn hơn 0")
    @JsonProperty("weight")
    private Integer weight; // Đơn vị: gram
    
    @JsonProperty("deliver_option")
    private String deliverOption; // "xteam" hoặc "none" (mặc định: xteam)
    
    @JsonProperty("transport")
    private String transport; // "road" hoặc "fly" (mặc định: road)
}

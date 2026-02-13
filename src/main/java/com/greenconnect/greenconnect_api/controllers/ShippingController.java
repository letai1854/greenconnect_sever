package com.greenconnect.greenconnect_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.ShippingFeeRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.ShippingFeeResponse;
import com.greenconnect.greenconnect_api.services.ShippingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipping Controller - Quản lý vận chuyển
 * Tích hợp GHTK API để tính phí ship
 */
@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {
    
    private final ShippingService shippingService;
    
    /**
     * Tính phí vận chuyển qua GHTK API
     * <p>Public endpoint - Không yêu cầu đăng nhập</p>
     * 
     * @param request Thông tin địa chỉ gửi, nhận và khối lượng (gram)
     * @return Phí vận chuyển và các thông tin chi tiết
     * 
     * Example request:
     * {
     *   "pick_province": "Thành phố Hồ Chí Minh",
     *   "pick_district": "Quận 7",
     *   "pick_ward": "Tân Quy",
     *   "province": "Thành phố Hồ Chí Minh",
     *   "district": "Quận 5",
     *   "ward": "Tân Thủy",
     *   "weight": 2000,
     *   "deliver_option": "xteam",
     *   "transport": "road"
     * }
     */
    @PostMapping("/calculate-fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateShippingFee(
            @Valid @RequestBody ShippingFeeRequest request) {
        log.info("POST /shipping/calculate-fee - Tính phí vận chuyển từ {}, {} đến {}, {} (khối lượng: {}g)", 
            request.getPickProvince(), request.getPickDistrict(),
            request.getProvince(), request.getDistrict(), request.getWeight());
        
        ShippingFeeResponse response = shippingService.calculateShippingFee(request);
        
        String message = String.format("Phí vận chuyển: %s (Khoảng cách: %.2f km)", 
            response.getFee().getOptions().getShipMoneyText(),
            response.getFee().getDistance());
        
        ApiResponse<ShippingFeeResponse> apiResponse = ResponseUtil.success(response, message);
        
        return ResponseEntity.ok(apiResponse);
    }
}

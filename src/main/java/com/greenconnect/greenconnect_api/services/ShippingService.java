package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.ShippingFeeRequest;
import com.greenconnect.greenconnect_api.dtos.response.ShippingFeeResponse;

/**
 * Service để tính phí vận chuyển qua GHTK API
 */
public interface ShippingService {
    
    /**
     * Tính phí vận chuyển từ địa điểm gửi đến địa điểm nhận
     * 
     * @param request Thông tin địa chỉ gửi, nhận và khối lượng
     * @return Thông tin phí vận chuyển từ GHTK
     */
    ShippingFeeResponse calculateShippingFee(ShippingFeeRequest request);
}

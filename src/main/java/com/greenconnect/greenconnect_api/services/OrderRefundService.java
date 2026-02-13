package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRefund;
import com.greenconnect.greenconnect_api.dtos.request.RequestOrdersRefund;
import com.greenconnect.greenconnect_api.dtos.request.UpdateOrderRequestAdmin;
import com.greenconnect.greenconnect_api.dtos.response.OrderRefundRespone;

import java.util.UUID;
import java.util.List;

public interface OrderRefundService {
    OrderRefundRespone create(CreateOrderRefund request);
    OrderRefundRespone update(UUID id, RequestOrdersRefund request);
    // Backwards-compatible admin-specific method (some controllers may call this)
    OrderRefundRespone updateAdmin(UUID id, UpdateOrderRequestAdmin request);
    void delete(UUID id);
    // List active (isStatus = true)
    List<OrderRefundRespone> findAllActive();
    // List all, includeInactive=false returns only active, includeInactive=true returns all
    List<OrderRefundRespone> findAll(Boolean includeInactive);
}


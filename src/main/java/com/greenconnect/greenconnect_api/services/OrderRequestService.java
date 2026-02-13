package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dto.OrderRequestDTO;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderRequestService {
    
    /**
     * Create a new order request
     * @param orderRequestDTO Order request data
     * @return Created order request
     */
    OrderRequestDTO createOrderRequest(OrderRequestDTO orderRequestDTO);
    
    /**
     * Update an existing order request (only non-null fields)
     * @param id Order request ID
     * @param orderRequestDTO Updated order request data
     * @return Updated order request
     */
    OrderRequestDTO updateOrderRequest(UUID id, OrderRequestDTO orderRequestDTO);
    
    /**
     * Get order request by ID
     * @param id Order request ID
     * @return Order request details
     */
    OrderRequestDTO getOrderRequestById(UUID id);
    
    /**
     * Get paginated order requests by user ID and status
     * @param userId User ID
     * @param status Request status (DANG_XU_LY, HOAN_THANH)
     * @param pageable Pagination parameters
     * @return Page of order requests
     */
    Page<OrderRequestDTO> getOrderRequestsByUserAndStatus(UUID userId, RequestStatus status, Pageable pageable);
    
    /**
     * Get all order requests with filter and pagination
     * @param filter Filter type (all, DANG_XU_LY, HOAN_THANH)
     * @param page Page number
     * @param size Page size
     * @return Page of order requests
     */
    Page<OrderRequestDTO> getAllOrderRequestsWithFilter(String filter, int page, int size);
    
    /**
     * Search order requests by keyword
     * @param keyword Search keyword
     * @param filter Filter type (all, DANG_XU_LY, HOAN_THANH)
     * @param page Page number
     * @param size Page size
     * @return Page of order requests
     */
    Page<OrderRequestDTO> searchOrderRequests(String keyword, String filter, int page, int size);
}

package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dto.OrderRequestDTO;
import com.greenconnect.greenconnect_api.dto.RequestMediaDTO;
import com.greenconnect.greenconnect_api.entities.Order;
import com.greenconnect.greenconnect_api.entities.OrderRequest;
import com.greenconnect.greenconnect_api.entities.RequestMedia;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.CreatedBy;
import com.greenconnect.greenconnect_api.enums.MediaType;
import com.greenconnect.greenconnect_api.enums.RefundStatus;
import com.greenconnect.greenconnect_api.enums.RequestStatus;
import com.greenconnect.greenconnect_api.enums.RequestType;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.OrderRepository;
import com.greenconnect.greenconnect_api.repositories.OrderRequestRepository;
import com.greenconnect.greenconnect_api.repositories.RequestMediaRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.OrderRequestService;
import com.greenconnect.greenconnect_api.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderRequestServiceImpl implements OrderRequestService {

    private final OrderRequestRepository orderRequestRepository;
    private final RequestMediaRepository requestMediaRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderRequestDTO createOrderRequest(OrderRequestDTO dto) {
        OrderRequest orderRequest = new OrderRequest();
        
        // ✅ AUTO-GENERATE: UUID id (handled by @GeneratedValue)
        // ✅ AUTO-GENERATE: request_id (unique Long)
        Long nextRequestId = generateNextRequestId();
        orderRequest.setRequestId(nextRequestId);
        
        // ✅ AUTO-SET: userId from JWT token (not from DTO)
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + currentUserId));
        orderRequest.setUser(user);
        
        Order order = null;
        if (dto.getOrderId() != null) {
            order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + dto.getOrderId()));
            orderRequest.setOrder(order);
        }
        
        // ✅ Validate orderCode nếu người dùng nhập
        if (dto.getOrderCode() != null && !dto.getOrderCode().trim().isEmpty()) {
            Order orderByCode = orderRepository.findByOrderCode(dto.getOrderCode())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found with code: " + dto.getOrderCode()));
            
            // Nếu đã có orderId và orderCode không khớp với order đó → lỗi
            if (order != null && !order.getOrderCode().equals(dto.getOrderCode())) {
                throw new AppException(ErrorCode.BAD_REQUEST, 
                    "Order code '" + dto.getOrderCode() + "' does not match with order ID '" + dto.getOrderId() + "'");
            }
            
            // Set order từ orderCode nếu chưa có orderId
            if (order == null) {
                order = orderByCode;
                orderRequest.setOrder(order);
            }
            
            orderRequest.setOrderCode(dto.getOrderCode());
        }
        
        // Set optional fields (only if not null)
        if (dto.getRequestType() != null) {
            validateRequestType(dto.getRequestType());
            orderRequest.setRequestType(dto.getRequestType());
            
            // ✅ AUTO-FILL: Nếu là HOAN_TIEN và có order, tự động điền thông tin tiền từ order
            if (dto.getRequestType() == RequestType.HOAN_TIEN && order != null) {
                // Auto-fill refund amount từ order total payment
                if (dto.getRefundAmount() == null && order.getTotalPayment() != null) {
                    orderRequest.setRefundAmount(order.getTotalPayment());
                }
                
                // Auto-fill refund status mặc định là DANG_CHO
                if (dto.getRefundStatus() == null) {
                    orderRequest.setRefundStatus(RefundStatus.DANG_CHO);
                }
            }
        }
        
        // ✅ AUTO-SET: createdBy - always USER for customer-initiated requests
        orderRequest.setCreatedBy(CreatedBy.USER);
        
        // ✅ AUTO-SET: timestamps (fix for @CreationTimestamp not working)
        orderRequest.setCreatedAt(LocalDateTime.now());
        orderRequest.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getReason() != null) {
            orderRequest.setReason(dto.getReason());
        }
        
        if (dto.getEmail() != null) {
            orderRequest.setEmail(dto.getEmail());
        }
        
        if (dto.getPhoneNumber() != null) {
            orderRequest.setPhoneNumber(dto.getPhoneNumber());
        }
        
        if (dto.getStatus() != null) {
            validateRequestStatus(dto.getStatus());
            orderRequest.setStatus(dto.getStatus());
        } else {
            orderRequest.setStatus(RequestStatus.DANG_XU_LY); // Default status
        }
        
        // ✅ RefundAmount và RefundStatus sẽ được auto-fill nếu là HOAN_TIEN
        // Chỉ override nếu DTO có giá trị
        // ✅ RefundAmount và RefundStatus sẽ được auto-fill nếu là HOAN_TIEN
        // Chỉ override nếu DTO có giá trị
        if (dto.getRefundAmount() != null) {
            orderRequest.setRefundAmount(dto.getRefundAmount());
        }
        
        if (dto.getRefundStatus() != null) {
            validateRefundStatus(dto.getRefundStatus());
            orderRequest.setRefundStatus(dto.getRefundStatus());
        }
        
        // ✅ CHỈ LƯU THÔNG TIN NGÂN HÀNG NẾU KHÔNG PHẢI VNPAY
        // VNPay tự động hoàn tiền về tài khoản VNPay của khách, không cần thông tin ngân hàng thủ công
        boolean isVnpayOrder = order != null && 
                               order.getPaymentMethod() == com.greenconnect.greenconnect_api.enums.PaymentMethod.VNPAY;
        
        if (!isVnpayOrder) {
            // Chỉ lưu thông tin ngân hàng cho đơn COD hoặc các phương thức thanh toán khác
            if (dto.getCustomerBankAccountName() != null) {
                orderRequest.setCustomerBankAccountName(dto.getCustomerBankAccountName());
            }
            
            if (dto.getCustomerBankAccountNumber() != null) {
                orderRequest.setCustomerBankAccountNumber(dto.getCustomerBankAccountNumber());
            }
            
            if (dto.getCustomerBankName() != null) {
                orderRequest.setCustomerBankName(dto.getCustomerBankName());
            }
            
            if (dto.getCustomerBankCode() != null) {
                orderRequest.setCustomerBankCode(dto.getCustomerBankCode());
            }
        } else {
            // VNPay: Bỏ qua thông tin ngân hàng, set null để đảm bảo không lưu nhầm
            orderRequest.setCustomerBankAccountName(null);
            orderRequest.setCustomerBankAccountNumber(null);
            orderRequest.setCustomerBankName(null);
            orderRequest.setCustomerBankCode(null);
        }
        
        if (dto.getProcessedByAdminId() != null) {
            User admin = userRepository.findById(dto.getProcessedByAdminId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Admin not found with id: " + dto.getProcessedByAdminId()));
            orderRequest.setProcessedByAdmin(admin);
        }
        
        if (dto.getReply() != null) {
            orderRequest.setReply(dto.getReply());
        }
        
        // Save order request
        OrderRequest savedRequest = orderRequestRepository.save(orderRequest);
        
        // Handle request medias
        if (dto.getRequestMedias() != null && !dto.getRequestMedias().isEmpty()) {
            List<RequestMedia> medias = new ArrayList<>();
            for (RequestMediaDTO mediaDTO : dto.getRequestMedias()) {
                if (mediaDTO.getMediaUrl() != null && mediaDTO.getMediaType() != null) {
                    validateMediaType(mediaDTO.getMediaType());
                    RequestMedia media = RequestMedia.builder()
                        .orderRequest(savedRequest)
                        .mediaUrl(mediaDTO.getMediaUrl())
                        .mediaType(mediaDTO.getMediaType())
                        .build();
                    medias.add(media);
                }
            }
            if (!medias.isEmpty()) {
                requestMediaRepository.saveAll(medias);
                savedRequest.setRequestMedias(medias);
            }
        }
        
        return mapToDTO(savedRequest);
    }

    @Override
    @Transactional
    public OrderRequestDTO updateOrderRequest(UUID id, OrderRequestDTO dto) {
        OrderRequest orderRequest = orderRequestRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order request not found with id: " + id));
        
        // Update fields only if not null
        if (dto.getOrderId() != null) {
            Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + dto.getOrderId()));
            orderRequest.setOrder(order);
        }
        
        if (dto.getOrderCode() != null) {
            orderRequest.setOrderCode(dto.getOrderCode());
        }
        
        if (dto.getRequestType() != null) {
            validateRequestType(dto.getRequestType());
            orderRequest.setRequestType(dto.getRequestType());
        }
        
        if (dto.getCreatedBy() != null) {
            validateCreatedBy(dto.getCreatedBy());
            orderRequest.setCreatedBy(dto.getCreatedBy());
        }
        
        if (dto.getReason() != null) {
            orderRequest.setReason(dto.getReason());
        }
        
        if (dto.getEmail() != null) {
            orderRequest.setEmail(dto.getEmail());
        }
        
        if (dto.getPhoneNumber() != null) {
            orderRequest.setPhoneNumber(dto.getPhoneNumber());
        }
        
        if (dto.getStatus() != null) {
            validateRequestStatus(dto.getStatus());
            orderRequest.setStatus(dto.getStatus());
        }
        
        if (dto.getRefundAmount() != null) {
            orderRequest.setRefundAmount(dto.getRefundAmount());
        }
        
        if (dto.getRefundStatus() != null) {
            validateRefundStatus(dto.getRefundStatus());
            orderRequest.setRefundStatus(dto.getRefundStatus());
        }
        
        // ✅ CHỈ CẬP NHẬT THÔNG TIN NGÂN HÀNG NẾU KHÔNG PHẢI VNPAY
        // VNPay tự động hoàn tiền về tài khoản VNPay của khách, không cần thông tin ngân hàng thủ công
        Order currentOrder = orderRequest.getOrder();
        boolean isVnpayOrder = currentOrder != null && 
                               currentOrder.getPaymentMethod() == com.greenconnect.greenconnect_api.enums.PaymentMethod.VNPAY;
        
        if (!isVnpayOrder) {
            // Chỉ cập nhật thông tin ngân hàng cho đơn COD hoặc các phương thức thanh toán khác
            if (dto.getCustomerBankAccountName() != null) {
                orderRequest.setCustomerBankAccountName(dto.getCustomerBankAccountName());
            }
            
            if (dto.getCustomerBankAccountNumber() != null) {
                orderRequest.setCustomerBankAccountNumber(dto.getCustomerBankAccountNumber());
            }
            
            if (dto.getCustomerBankName() != null) {
                orderRequest.setCustomerBankName(dto.getCustomerBankName());
            }
            
            if (dto.getCustomerBankCode() != null) {
                orderRequest.setCustomerBankCode(dto.getCustomerBankCode());
            }
        } else {
            // VNPay: Xóa thông tin ngân hàng nếu có (đảm bảo không lưu nhầm)
            orderRequest.setCustomerBankAccountName(null);
            orderRequest.setCustomerBankAccountNumber(null);
            orderRequest.setCustomerBankName(null);
            orderRequest.setCustomerBankCode(null);
        }
        
        if (dto.getProcessedByAdminId() != null) {
            User admin = userRepository.findById(dto.getProcessedByAdminId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Admin not found with id: " + dto.getProcessedByAdminId()));
            orderRequest.setProcessedByAdmin(admin);
        }
        
        if (dto.getReply() != null) {
            orderRequest.setReply(dto.getReply());
        }
        
        // Update medias if provided
        if (dto.getRequestMedias() != null) {
            // Remove old medias
            if (orderRequest.getRequestMedias() != null && !orderRequest.getRequestMedias().isEmpty()) {
                requestMediaRepository.deleteAll(orderRequest.getRequestMedias());
            }
            
            // Add new medias
            List<RequestMedia> medias = new ArrayList<>();
            for (RequestMediaDTO mediaDTO : dto.getRequestMedias()) {
                if (mediaDTO.getMediaUrl() != null && mediaDTO.getMediaType() != null) {
                    validateMediaType(mediaDTO.getMediaType());
                    RequestMedia media = RequestMedia.builder()
                        .orderRequest(orderRequest)
                        .mediaUrl(mediaDTO.getMediaUrl())
                        .mediaType(mediaDTO.getMediaType())
                        .build();
                    medias.add(media);
                }
            }
            if (!medias.isEmpty()) {
                requestMediaRepository.saveAll(medias);
                orderRequest.setRequestMedias(medias);
            }
        }
        
        OrderRequest updatedRequest = orderRequestRepository.save(orderRequest);
        return mapToDTO(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderRequestDTO getOrderRequestById(UUID id) {
        OrderRequest orderRequest = orderRequestRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order request not found with id: " + id));
        return mapToDTO(orderRequest);
    }
    
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderRequestDTO> getOrderRequestsByUserAndStatus(
            UUID userId, RequestStatus status, org.springframework.data.domain.Pageable pageable) {
        
        // Validate user exists
        userRepository.findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + userId));
        
        // Find order requests by user and status with pagination
        org.springframework.data.domain.Page<OrderRequest> orderRequests;
        
        if (status != null) {
            // Filter by status
            orderRequests = orderRequestRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            // Get all statuses
            orderRequests = orderRequestRepository.findByUserId(userId, pageable);
        }
        
        // Map to DTOs
        return orderRequests.map(this::mapToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderRequestDTO> getAllOrderRequestsWithFilter(
            String filter, int page, int size) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        
        org.springframework.data.domain.Page<OrderRequest> orderRequests;
        
        switch (filter.toLowerCase()) {
            case "dang_xu_ly":
                orderRequests = orderRequestRepository.findByStatus(RequestStatus.DANG_XU_LY, pageable);
                break;
            case "hoan_thanh":
                orderRequests = orderRequestRepository.findByStatus(RequestStatus.HOAN_THANH, pageable);
                break;
            case "all":
            default:
                orderRequests = orderRequestRepository.findAll(pageable);
                break;
        }
        
        return orderRequests.map(this::mapToDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderRequestDTO> searchOrderRequests(
            String keyword, String filter, int page, int size) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        
        org.springframework.data.domain.Page<OrderRequest> orderRequests;
        
        if (filter.equalsIgnoreCase("all")) {
            // Search all statuses
            orderRequests = orderRequestRepository.searchByKeyword(keyword, pageable);
        } else {
            // Search with specific status
            RequestStatus status;
            if (filter.equalsIgnoreCase("dang_xu_ly")) {
                status = RequestStatus.DANG_XU_LY;
            } else if (filter.equalsIgnoreCase("hoan_thanh")) {
                status = RequestStatus.HOAN_THANH;
            } else {
                // Invalid filter, search all
                orderRequests = orderRequestRepository.searchByKeyword(keyword, pageable);
                return orderRequests.map(this::mapToDTO);
            }
            orderRequests = orderRequestRepository.searchByKeywordAndStatus(keyword, status, pageable);
        }
        
        return orderRequests.map(this::mapToDTO);
    }
    
    // ========== VALIDATION METHODS ==========
    
    private void validateRequestType(RequestType requestType) {
        if (requestType == null) {
            throw new IllegalArgumentException("Request type cannot be null");
        }
        // Check if enum value is valid
        try {
            RequestType.valueOf(requestType.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + requestType);
        }
    }
    
    private void validateCreatedBy(CreatedBy createdBy) {
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by cannot be null");
        }
        try {
            CreatedBy.valueOf(createdBy.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid created by value: " + createdBy);
        }
    }
    
    private void validateRequestStatus(RequestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Request status cannot be null");
        }
        try {
            RequestStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request status: " + status);
        }
    }
    
    private void validateRefundStatus(RefundStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Refund status cannot be null");
        }
        try {
            RefundStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid refund status: " + status);
        }
    }
    
    private void validateMediaType(MediaType mediaType) {
        if (mediaType == null) {
            throw new IllegalArgumentException("Media type cannot be null");
        }
        try {
            MediaType.valueOf(mediaType.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }
    }
    
    // ========== MAPPING METHODS ==========
    
    private OrderRequestDTO mapToDTO(OrderRequest orderRequest) {
        if (orderRequest == null) {
            return null;
        }
        
        List<RequestMediaDTO> mediaDTOs = null;
        if (orderRequest.getRequestMedias() != null) {
            mediaDTOs = orderRequest.getRequestMedias().stream()
                .map(this::mapMediaToDTO)
                .collect(Collectors.toList());
        }
        
        return OrderRequestDTO.builder()
            .id(orderRequest.getId())
            .requestId(orderRequest.getRequestId())
            .orderCode(orderRequest.getOrderCode())
            .orderId(orderRequest.getOrder() != null ? orderRequest.getOrder().getId() : null)
            .userId(orderRequest.getUser() != null ? orderRequest.getUser().getId() : null)
            .requestType(orderRequest.getRequestType())
            .createdBy(orderRequest.getCreatedBy())
            .reason(orderRequest.getReason())
            .email(orderRequest.getEmail())
            .phoneNumber(orderRequest.getPhoneNumber())
            .status(orderRequest.getStatus())
            .refundAmount(orderRequest.getRefundAmount())
            .refundStatus(orderRequest.getRefundStatus())
            .customerBankAccountName(orderRequest.getCustomerBankAccountName())
            .customerBankAccountNumber(orderRequest.getCustomerBankAccountNumber())
            .customerBankName(orderRequest.getCustomerBankName())
            .customerBankCode(orderRequest.getCustomerBankCode())
            .processedByAdminId(orderRequest.getProcessedByAdmin() != null ? orderRequest.getProcessedByAdmin().getId() : null)
            .reply(orderRequest.getReply())
            .requestMedias(mediaDTOs)
            .createdAt(orderRequest.getCreatedAt())
            .updatedAt(orderRequest.getUpdatedAt())
            .build();
    }
    
    private RequestMediaDTO mapMediaToDTO(RequestMedia media) {
        if (media == null) {
            return null;
        }
        
        return RequestMediaDTO.builder()
            .id(media.getId())
            .orderRequestId(media.getOrderRequest() != null ? media.getOrderRequest().getId() : null)
            .mediaUrl(media.getMediaUrl())
            .mediaType(media.getMediaType())
            .build();
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Generate next unique request ID
     */
    private Long generateNextRequestId() {
        // Get max request_id from database
        Long maxId = orderRequestRepository.findAll()
            .stream()
            .map(OrderRequest::getRequestId)
            .filter(id -> id != null)
            .max(Long::compareTo)
            .orElse(0L);
        
        return maxId + 1;
    }
}

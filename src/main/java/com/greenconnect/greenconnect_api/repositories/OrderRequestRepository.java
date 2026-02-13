package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.OrderRequest;
import com.greenconnect.greenconnect_api.enums.RequestStatus;

@Repository
public interface OrderRequestRepository extends JpaRepository<OrderRequest, UUID> {
    
    /**
     * Find all order requests by status (no pagination)
     */
    List<OrderRequest> findByStatus(RequestStatus status);
    
    /**
     * Find all order requests by status with pagination
     */
    Page<OrderRequest> findByStatus(RequestStatus status, Pageable pageable);
    
    /**
     * Find order requests by user ID and status with pagination
     */
    Page<OrderRequest> findByUserIdAndStatus(UUID userId, RequestStatus status, Pageable pageable);
    
    /**
     * Find all order requests by user ID with pagination (all statuses)
     */
    Page<OrderRequest> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Search order requests by keyword in reason, email, phone, orderCode
     */
    @Query("SELECT o FROM OrderRequest o WHERE " +
        "LOWER(o.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "CAST(o.requestId AS string) LIKE CONCAT('%', :keyword, '%')")
    Page<OrderRequest> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Search order requests by keyword and status
     */
    @Query("SELECT o FROM OrderRequest o WHERE " +
        "o.status = :status AND (" +
        "LOWER(o.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
        "CAST(o.requestId AS string) LIKE CONCAT('%', :keyword, '%'))")
    Page<OrderRequest> searchByKeywordAndStatus(
        @Param("keyword") String keyword,
        @Param("status") RequestStatus status,
        Pageable pageable);
    
    /**
     * Check if a HOAN_TIEN request already exists for an order (not completed/rejected)
     * Used to prevent spam - user can only have ONE pending refund request per order
     */
    boolean existsByOrder_IdAndRequestTypeAndStatusNot(UUID orderId, com.greenconnect.greenconnect_api.enums.RequestType requestType, RequestStatus status);
    
    /**
     * Check if ANY request exists for an order with specific request type and status
     */
    boolean existsByOrder_IdAndRequestType(UUID orderId, com.greenconnect.greenconnect_api.enums.RequestType requestType);
    
    /**
     * Count pending HOAN_TIEN requests for an order
     */
    @Query("SELECT COUNT(o) FROM OrderRequest o WHERE o.order.id = :orderId AND o.requestType = :requestType AND o.status = :status")
    long countByOrderIdAndRequestTypeAndStatus(
        @Param("orderId") UUID orderId, 
        @Param("requestType") com.greenconnect.greenconnect_api.enums.RequestType requestType,
        @Param("status") RequestStatus status);
}

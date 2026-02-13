package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.OrderVoucher;

/**
 * Repository for OrderVoucher entity (Many-to-Many relationship)
 */
@Repository
public interface OrderVoucherRepository extends JpaRepository<OrderVoucher, UUID> {
    
    /**
     * Lấy tất cả voucher của một order
     */
    @Query("SELECT ov FROM OrderVoucher ov JOIN FETCH ov.voucher WHERE ov.order.id = :orderId")
    List<OrderVoucher> findByOrderIdWithVoucher(@Param("orderId") UUID orderId);
    
    /**
     * Kiểm tra order đã sử dụng voucher này chưa
     */
    boolean existsByOrderIdAndVoucherId(UUID orderId, UUID voucherId);
    
    /**
     * Xóa tất cả voucher của order (khi hủy đơn)
     */
    void deleteByOrderId(UUID orderId);
}

package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.VoucherUsage;

/**
 * Repository interface cho thực thể VoucherUsage.
 * 
 * <p>Bảng này đóng vai trò như một "sổ nhật ký", ghi lại mỗi lần một voucher được
 * một người dùng sử dụng. Repository này cung cấp các phương thức quan trọng để
 * kiểm tra các giới hạn sử dụng và truy vấn lịch sử.</p>
 */
@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {

    /**
     * Đếm số lần một người dùng cụ thể đã sử dụng một voucher cụ thể.
     * <p>Đây là phương thức cốt lõi để thực thi quy tắc "mỗi người dùng chỉ được sử dụng
     * voucher X một lần". Trong {@code OrderService}, trước khi áp dụng voucher, ta sẽ gọi
     * hàm này. Nếu kết quả > 0, có nghĩa là người dùng đã sử dụng voucher này trước đó.</p>
     *
    //  * @param voucher Voucher cần kiểm tra.
    //  * @param user    Người dùng cần kiểm tra.
    //  * @return Số nguyên (long) là số lần người dùng đã sử dụng voucher. Thường là 0 hoặc 1.
    //  */
    // long countByVoucherAndUser(Voucher voucher, User user);

    // /**
    //  * Đếm tổng số lần một voucher đã được sử dụng bởi tất cả người dùng.
    //  * <p>Hàm này hữu ích để kiểm tra giới hạn sử dụng chung của một voucher (ví dụ: "chỉ dành
    //  * cho 100 khách hàng đầu tiên"). Mặc dù có thể dùng trường `usageCount` trên chính
    //  * thực thể Voucher, việc truy vấn trực tiếp từ đây đảm bảo tính chính xác tuyệt đối.</p>
    //  *
    //  * @param voucher Voucher cần đếm.
    //  * @return Tổng số lần voucher đã được sử dụng.
    //  */
    // long countByVoucher(Voucher voucher);

    // /**
    //  * Lấy danh sách lịch sử sử dụng voucher của một người dùng cụ thể.
    //  * <p>Hữu ích cho các chức năng như "Ví voucher của tôi" hoặc "Xem lại các ưu đãi đã dùng".</p>
    //  *
    //  * @param user Người dùng cần xem lịch sử.
    //  * @return Một danh sách (List) các bản ghi {@link VoucherUsage}.
    //  */
    // List<VoucherUsage> findByUser(User user);
    
    /**
     * Tìm tất cả VoucherUsage của một user với một voucher cụ thể
     * 
     * @param voucherId ID của voucher
     * @param userId ID của user
     * @return Danh sách VoucherUsage
     */
    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId AND vu.user.id = :userId ORDER BY vu.usageTime DESC")
    List<VoucherUsage> findByVoucherIdAndUserId(@Param("voucherId") UUID voucherId, @Param("userId") UUID userId);
    
    /**
     * Đếm số lần user đã sử dụng voucher cụ thể
     * 
     * @param voucherId ID của voucher
     * @param userId ID của user
     * @return Số lần đã sử dụng
     */
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher.id = :voucherId AND vu.user.id = :userId")
    long countByVoucherIdAndUserId(@Param("voucherId") UUID voucherId, @Param("userId") UUID userId);

    /**
     * Tìm bản ghi sử dụng voucher dựa trên đơn hàng và người dùng.
     * <p>Rất quan trọng cho chức năng HỦY ĐƠN HÀNG. Khi một đơn hàng bị hủy, ta cần tìm
     * đúng bản ghi VoucherUsage này để XÓA nó đi, qua đó "hoàn lại" lượt sử dụng
     * voucher cho khách hàng.</p>
     *
     * @param orderId ID của đơn hàng đã bị hủy.
     * @param userId  ID của người dùng đã hủy đơn.
     * @return Bản ghi VoucherUsage cần xóa.
     */
    // Lưu ý: Tên cột trong Entity phải là 'order' và 'user'
    // Ví dụ: findByOrderIdAndUserId(UUID orderId, UUID userId);
    // Hoặc nếu bạn liên kết trực tiếp với Entity Order:
    // Optional<VoucherUsage> findByOrder(Order order);
}
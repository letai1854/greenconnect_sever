package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.DeliveryOption;

/**
 * Repository interface cho thực thể DeliveryOption.
 * 
 * <p>Quản lý các phương thức giao hàng mà cửa hàng cung cấp (ví dụ: Giao hàng
 * tiêu chuẩn, Giao hàng hỏa tốc). Bảng này cho phép Admin tùy chỉnh phí ship,
 * thời gian giao hàng dự kiến mà không cần thay đổi code.</p>
 */
@Repository
public interface DeliveryOptionRepository extends JpaRepository<DeliveryOption, UUID> {

    /**
     * Lấy danh sách tất cả các phương thức giao hàng đang ở trạng thái hoạt động.
     * <p>Phương thức này được sử dụng trong quy trình thanh toán (checkout).
     * Sau khi khách hàng nhập địa chỉ, hệ thống sẽ gọi hàm này để hiển thị
     * danh sách các lựa chọn giao hàng hợp lệ cho khách hàng lựa chọn.</p>
     *
     * @return một danh sách (List) các đối tượng {@link DeliveryOption} đang hoạt động.
     */
    List<DeliveryOption> findByIsActiveTrue();

    // Nếu cần có sắp xếp hoặc phân trang, sẽ thêm method overload hoặc Pageable sau này.

}
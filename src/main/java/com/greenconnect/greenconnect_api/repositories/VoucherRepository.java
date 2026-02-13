package com.greenconnect.greenconnect_api.repositories; 
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Voucher;

/**
 * Repository interface cho thực thể Voucher.
 * 
 * <p>Cung cấp các hàm CRUD cơ bản (tạo, sửa, xóa voucher bởi Admin) và các phương thức truy vấn
 * tùy chỉnh để tìm kiếm và xác thực voucher.
 * Kế thừa {@link JpaSpecificationExecutor} để hỗ trợ chức năng lọc/tìm kiếm voucher
 * nâng cao trong trang quản trị.</p>
 */
@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID>, JpaSpecificationExecutor<Voucher> {

    /**
     * Tìm kiếm một voucher dựa trên mã code mà người dùng nhập vào.
     * <p>Đây là phương thức cốt lõi và quan trọng nhất, được sử dụng trong {@code OrderService}
     * khi khách hàng áp dụng mã giảm giá vào giỏ hàng.
     * Logic nghiệp vụ trong Service sẽ sử dụng voucher tìm thấy từ phương thức này để
     * kiểm tra các điều kiện khác (còn hạn sử dụng, còn lượt dùng, có active không...).</p>
     *
     * <p>Voucher code thường phân biệt chữ hoa/thường, nên chúng ta không dùng IgnoreCase ở đây.</p>
     *
    //  * @param code Mã voucher mà khách hàng nhập (ví dụ: "SALE50K").
    //  * @return một đối tượng {@link Optional} chứa {@link Voucher} nếu tìm thấy, ngược lại trả về Optional rỗng.
    //  */
    // Optional<Voucher> findByCode(String code);

    /*
     * LƯU Ý VỀ TÌM KIẾM VÀ LỌC CHO ADMIN:
     *
     * Tương tự như UserRepository, thay vì tạo nhiều hàm tìm kiếm phức tạp trong này,
     * chúng ta sẽ sử dụng JpaSpecificationExecutor.
     *
     * Lớp VoucherService sẽ xây dựng một đối tượng Specification linh hoạt dựa trên các
     * tiêu chí mà Admin muốn lọc (ví dụ: lọc theo trạng thái active/inactive,
     * lọc theo loại voucher PERCENTAGE/FIXED_AMOUNT, tìm kiếm theo code...).
     *
     * Phương thức được sử dụng sẽ là: findAll(Specification<Voucher> spec, Pageable pageable)
     */
}
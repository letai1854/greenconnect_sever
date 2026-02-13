package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.UserStatus;

// import co.elastic.clients.elasticsearch.security.User;
/**
 * Repository interface cho thực thể User.
 * 
 * <p>Kế thừa từ {@link JpaRepository} để có các phương thức CRUD và phân trang cơ bản.
 * Kế thừa từ {@link JpaSpecificationExecutor} để cho phép xây dựng các truy vấn động,
 * phức tạp cho chức năng tìm kiếm và lọc người dùng, thay vì phải viết nhiều hàm truy vấn
 * tùy chỉnh trong interface này.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /**
     * Tìm kiếm một người dùng dựa trên địa chỉ email.
     * <p>Đây là phương thức cốt lõi cho chức năng đăng nhập, kiểm tra người dùng từ Firebase,
     * và được Spring Security sử dụng trong {@code CustomUserDetailsService}. Việc sử dụng
     * {@link Optional} là một "best practice" để xử lý an toàn trường hợp không tìm thấy người dùng,
     * tránh lỗi {@code NullPointerException}.</p>
     *
     * @param email Địa chỉ email của người dùng cần tìm (không phân biệt chữ hoa/thường).
     * @return một đối tượng {@link Optional} chứa {@link User} nếu tìm thấy, ngược lại trả về Optional rỗng.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Kiểm tra nhanh xem một địa chỉ email đã tồn tại trong hệ thống hay chưa.
     * <p>Phương thức này hiệu quả hơn việc dùng {@code findByEmail} rồi kiểm tra kết quả,
     * vì nó chỉ trả về true/false. Rất cần thiết cho chức năng đăng ký để ngăn chặn việc
     * tạo tài khoản với email đã được sử dụng.</p>
     * <p>Sử dụng index {@code idx_user_email} được định nghĩa trong User entity để tối ưu performance.</p>
     *
     * @param email Địa chỉ email cần kiểm tra.
     * @return {@code true} nếu email đã tồn tại, {@code false} nếu chưa tồn tại.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Tìm người dùng Firebase theo email và provider.
     * <p>Phương thức này được sử dụng trong chức năng đăng nhập Firebase để xác thực
     * người dùng dựa trên email và nhà cung cấp dịch vụ (Google, Facebook, Apple...).</p>
     *
     * @param email Địa chỉ email của người dùng (không phân biệt chữ hoa/thường).
     * @param provider Nhà cung cấp dịch vụ đăng nhập (GOOGLE, FACEBOOK, APPLE...).
     * @return một đối tượng {@link Optional} chứa {@link User} nếu tìm thấy, ngược lại trả về Optional rỗng.
     */
    Optional<User> findByEmailIgnoreCaseAndProvider(String email, com.greenconnect.greenconnect_api.enums.Provider provider);

    /**
     * Tìm users theo status với phân trang.
     * <p>Dùng cho invitation system để lấy danh sách user PENDING_ACTIVATION.</p>
     *
     * @param status Trạng thái user cần tìm (PENDING_ACTIVATION, ACTIVE, INACTIVE...).
     * @param pageable Thông tin phân trang và sắp xếp.
     * @return Page chứa danh sách users khớp với status.
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Lấy giá trị userCode lớn nhất trong database.
     * <p>Dùng để generate userCode tự động cho user mới (sequential).</p>
     *
     * @return giá trị userCode lớn nhất, hoặc null nếu chưa có user nào.
     */
    @org.springframework.data.jpa.repository.Query("SELECT MAX(u.userCode) FROM User u")
    Long findMaxUserCode();
    
    /**
     * Kiểm tra xem userCode đã tồn tại trong database chưa.
     * <p>Dùng để đảm bảo userCode được random là unique.</p>
     *
     * @param userCode Mã user cần kiểm tra.
     * @return {@code true} nếu userCode đã tồn tại, {@code false} nếu chưa tồn tại.
     */
    boolean existsByUserCode(Long userCode);

    /**
     * Lấy danh sách users không phải CUSTOMER role (tất cả staff roles).
     * <p>Dùng cho admin để xem danh sách tất cả nhân viên (ORDER_MANAGER, PRODUCT_MANAGER, etc.)</p>
     * <p>Query này lấy users mà có ít nhất 1 role không phải CUSTOMER và role đó còn active.</p>
     *
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Page chứa danh sách users không phải CUSTOMER role
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.userRoles ur " +
           "WHERE ur.role != com.greenconnect.greenconnect_api.enums.Role.CUSTOMER " +
           "AND ur.active = true " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    Page<User> findNonCustomerUsers(Pageable pageable);

    /**
     * Lấy danh sách users chỉ có CUSTOMER role.
     * <p>Query này lấy users mà có ít nhất 1 role là CUSTOMER và role đó còn active.</p>
     * <p>Sử dụng cho ADMIN và CUSTOMER_SUPPORT để xem danh sách khách hàng.</p>
     *
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Page chứa danh sách users có CUSTOMER role
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.userRoles ur " +
           "WHERE ur.role = com.greenconnect.greenconnect_api.enums.Role.CUSTOMER " +
           "AND ur.active = true " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    Page<User> findCustomerUsers(Pageable pageable);

    /**
     * Lấy danh sách user IDs có các role cụ thể (dùng cho notification system).
     * <p>Query này lấy tất cả user có ít nhất 1 trong các role được chỉ định và role đó còn active.</p>
     * <p>Sử dụng để gửi thông báo đến tất cả admin/manager trong hệ thống.</p>
     *
     * @param roles Danh sách roles cần tìm (VD: ADMIN, CUSTOMER_SUPPORT)
     * @return List UUID của các users có roles được chỉ định
     */
    @Query("SELECT DISTINCT u.id FROM User u " +
           "JOIN u.userRoles ur " +
           "WHERE ur.role IN :roles " +
           "AND ur.active = true " +
           "AND u.status = com.greenconnect.greenconnect_api.enums.UserStatus.ACTIVE " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    List<UUID> findUserIdsByRoles(@Param("roles") List<com.greenconnect.greenconnect_api.enums.Role> roles);

    /*
     * LƯU Ý QUAN TRỌNG VỀ TÌM KIẾM VÀ LỌC:
     *
     * Thay vì thêm các phương thức như:
     *   - Page<User> findByFullNameContaining(String keyword, Pageable pageable);
     *   - Page<User> findByRole(Role role, Pageable pageable);
     *   - Page<User> findByFullNameAndRole(String keyword, Role role, Pageable pageable);
     *
     * Chúng ta sử dụng JpaSpecificationExecutor. Lớp UserService sẽ chịu trách nhiệm
     * xây dựng một đối tượng Specification linh hoạt dựa trên các tiêu chí tìm kiếm mà Admin
     * gửi lên (ví dụ: tìm theo tên, email, vai trò, trạng thái...).
     *
     * Cách tiếp cận này giúp giữ cho Repository luôn gọn gàng và tập trung vào việc truy cập
     * dữ liệu, trong khi logic xây dựng truy vấn phức tạp được chuyển đến lớp Service.
     *
     * Phương thức được sử dụng sẽ là: findAll(Specification<User> spec, Pageable pageable)
     */
}
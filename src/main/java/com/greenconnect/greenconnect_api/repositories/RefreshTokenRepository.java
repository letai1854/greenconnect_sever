package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.RefreshToken;
import com.greenconnect.greenconnect_api.entities.User;

/**
 * Repository interface cho thực thể RefreshToken.
 * 
 * <p>Quản lý việc lưu trữ, truy vấn và xóa các Refresh Token. Đây là một phần
 * cốt lõi của cơ chế xác thực, cho phép người dùng duy trì phiên đăng nhập
 * một cách an toàn mà không cần phải đăng nhập lại thường xuyên.</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Tìm kiếm một Refresh Token dựa trên chuỗi token của nó.
     * <p>Đây là phương thức chính được sử dụng trong {@code AuthService} khi xử lý
     * yêu cầu làm mới Access Token. Hệ thống sẽ dùng token do client gửi lên để
     * tìm kiếm trong CSDL.</p>
     *
    //  * @param token Chuỗi Refresh Token cần tìm.
    //  * @return một đối tượng {@link Optional} chứa {@link RefreshToken} nếu tìm thấy.
    //  */
    // Optional<RefreshToken> findByToken(String token);

    // /**
    //  * Tìm kiếm Refresh Token của một người dùng cụ thể.
    //  * <p>Hữu ích trong các kịch bản như khi người dùng đăng nhập, hệ thống có thể
    //  * kiểm tra xem người dùng này đã có token cũ chưa để thu hồi (nếu chính sách
    //  * bảo mật yêu cầu mỗi user chỉ có một token active).</p>
    //  *
    //  * @param user Đối tượng người dùng.
    //  * @return một đối tượng {@link Optional} chứa {@link RefreshToken} của người dùng đó.
    //  */
    // Optional<RefreshToken> findByUser(User user);
    
    /**
     * Xóa Refresh Token của một người dùng cụ thể.
     * <p>Phương thức này rất quan trọng cho chức năng "Đăng xuất". Khi người dùng
     * nhấn đăng xuất, client sẽ xóa token trên thiết bị, đồng thời gọi API để
     * backend xóa Refresh Token trong CSDL, vô hiệu hóa hoàn toàn phiên đăng nhập đó.</p>
     *
     * @param user Người dùng cần xóa token.
     * @return Số lượng bản ghi đã bị xóa (thường là 1).
     */
    @Transactional
    @Modifying
    int deleteByUser(User user);
    
    /**
     * Xóa tất cả refresh token của user trừ token có ID cụ thể.
     * <p>Dùng để cleanup khi user có token còn hạn nhưng có nhiều token duplicate.</p>
     *
     * @param user User cần xóa token
     * @param tokenId ID của token cần giữ lại
     * @return Số lượng bản ghi đã bị xóa
     */
    @Transactional
    @Modifying
    int deleteByUserAndIdNot(User user, UUID tokenId);
    
    /**
     * Tìm kiếm refresh token theo user, token hash và thời gian hết hạn.
     * <p>Được sử dụng trong refresh token flow để verify token còn valid.</p>
     *
     * @param user User sở hữu token
     * @param tokenHash Hash của refresh token
     * @param currentTime Thời gian hiện tại để check expiry
     * @return Optional chứa RefreshToken nếu tìm thấy và chưa hết hạn
     */
    Optional<RefreshToken> findByUserAndTokenHashAndExpiryDateAfter(User user, String tokenHash, LocalDateTime currentTime);
    
    /**
     * Tìm kiếm refresh token còn hạn của một user (lấy token mới nhất).
     * <p>Được sử dụng trong login để kiểm tra xem user có token còn hạn không.</p>
     *
     * @param user User cần kiểm tra
     * @param currentTime Thời gian hiện tại để check expiry
     * @return Optional chứa RefreshToken mới nhất nếu tìm thấy và chưa hết hạn
     */
    Optional<RefreshToken> findFirstByUserAndExpiryDateAfterOrderByCreatedAtDesc(User user, LocalDateTime currentTime);

    Optional<RefreshToken> findByTokenHashAndExpiryDateAfter(String tokenHash, LocalDateTime now);
    
    // ⭐ TÌM tất cả tokens của user
    List<RefreshToken> findByUserAndExpiryDateAfter(User user, LocalDateTime now);
    
    // ⭐ XÓA tokens hết hạn
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    // ⭐ XÓA tokens cũ nhất khi quá giới hạn
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user ORDER BY rt.createdAt ASC")
    List<RefreshToken> findOldestTokensByUser(@Param("user") User user);
    
    // ⭐ ĐẾM số tokens active của user
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.expiryDate > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
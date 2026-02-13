package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.FcmToken;
import com.greenconnect.greenconnect_api.enums.DeviceType;

/**
 * Repository cho FCM Tokens
 */
@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    
    /**
     * Tìm FCM token theo token string
     * Dùng để kiểm tra token đã tồn tại chưa khi login
     */
    Optional<FcmToken> findByToken(String token);
    
    /**
     * Tìm tất cả FCM tokens của một user
     */
    List<FcmToken> findByUserId(UUID userId);
    
    /**
     * Tìm tất cả FCM tokens active của một user
     */
    List<FcmToken> findByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Tìm FCM token theo user và device type
     */
    Optional<FcmToken> findByUserIdAndDeviceType(UUID userId, DeviceType deviceType);
    
    /**
     * Xóa tất cả tokens của user (khi user logout all devices)
     */
    @Modifying
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f.user.id = :userId")
    int deactivateAllTokensByUserId(@Param("userId") UUID userId);
    
    /**
     * Xóa một token cụ thể (khi user logout device hiện tại)
     */
    @Modifying
    @Query("UPDATE FcmToken f SET f.isActive = false WHERE f.token = :token")
    int deactivateByToken(@Param("token") String token);
    
    /**
     * Xóa hẳn FCM token khỏi database (khi user logout)
     */
    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.token = :token")
    int deleteByToken(@Param("token") String token);
    
    /**
     * Xóa hẳn FCM token của user cụ thể (an toàn hơn - chỉ xóa nếu token thuộc đúng user)
     */
    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.user.id = :userId AND f.token = :token")
    int deleteByUserIdAndToken(@Param("userId") UUID userId, @Param("token") String token);
    
    /**
     * Kiểm tra token có tồn tại không
     */
    boolean existsByToken(String token);
}

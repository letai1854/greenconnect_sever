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

import com.greenconnect.greenconnect_api.entities.UserDevice;

/**
 * Repository interface cho thực thể UserDevice.
 * <p>Quản lý việc lưu trữ và truy vấn thông tin thiết bị của người dùng.</p>
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    /**
     * Tìm tất cả active devices của một user.
     */
    List<UserDevice> findByUserIdAndIsActive(UUID userId, Boolean isActive);

    /**
     * Tìm device theo FCM token.
     */
    Optional<UserDevice> findByFcmToken(String fcmToken);

    /**
     * Tìm tất cả devices của một user.
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id = :userId")
    List<UserDevice> findByUserId(UUID userId);

    /**
     * Xóa device của user theo id.
     */
    @Modifying
    void deleteByUserIdAndId(UUID userId, Long deviceId);

    /**
     * Xóa tất cả devices của user.
     */
    @Modifying
    @Query("DELETE FROM UserDevice ud WHERE ud.user.id = :userId")
    void deleteByUserId(UUID userId);

    /**
     * Xóa device theo FCM token.
     */
    @Modifying
    void deleteByFcmToken(String fcmToken);

    /**
     * Tìm device của user theo FCM token.
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id = :userId AND ud.fcmToken = :fcmToken")
    Optional<UserDevice> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    /**
     * Tìm devices sắp hết hạn (chưa cập nhật trong 30 ngày).
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.updatedAt < :cutoffDate")
    List<UserDevice> findInactiveDevices(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Đếm active devices của một user.
     */
    @Query("SELECT COUNT(ud) FROM UserDevice ud WHERE ud.user.id = :userId AND ud.isActive = :isActive")
    long countByUserIdAndIsActive(UUID userId, Boolean isActive);

    /**
     * Kiểm tra device tồn tại hay không.
     */
    boolean existsByFcmToken(String fcmToken);

    /**
     * Tìm devices của user theo type.
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id = :userId AND UPPER(CAST(ud.deviceType AS string)) = UPPER(:deviceType) ORDER BY ud.updatedAt DESC")
    List<UserDevice> findByUserIdAndDeviceType(@Param("userId") UUID userId, @Param("deviceType") String deviceType);

    /**
     * Đặt inactive tất cả devices khác của user.
     */
    @Modifying
    @Query("UPDATE UserDevice ud SET ud.isActive = false WHERE ud.user.id = :userId AND ud.id != :currentDeviceId")
    void deactivateOtherDevices(@Param("userId") UUID userId, @Param("currentDeviceId") Long currentDeviceId);
}


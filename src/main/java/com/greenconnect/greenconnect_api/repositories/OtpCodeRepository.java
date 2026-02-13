package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.OtpCode;
import com.greenconnect.greenconnect_api.entities.User;

import jakarta.transaction.Transactional;

/**
 * Repository interface cho thực thể OtpCode.
 * 
 * <p>Quản lý các mã OTP (One-Time Password) được tạo ra cho các mục đích
 * như xác thực tài khoản, đặt lại mật khẩu...</p>
 */
@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    
    /**
     * Tìm OTP code mới nhất cho user theo email
     */
    @Query("SELECT o FROM OtpCode o WHERE o.user.email = :email ORDER BY o.createdAt DESC")
    Optional<OtpCode> findLatestByUserEmail(@Param("email") String email);
    
    /**
     * Tìm OTP code theo user và mã OTP hash
     */
    @Query("SELECT o FROM OtpCode o WHERE o.user = :user AND o.otpCodeHash = :otpHash AND o.expiryDate > :currentTime")
    Optional<OtpCode> findValidOtpByUserAndHash(@Param("user") User user, 
                                               @Param("otpHash") String otpHash, 
                                               @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Xóa tất cả OTP codes cũ của user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpCode o WHERE o.user = :user")
    void deleteByUser(@Param("user") User user);
    
    /**
     * Xóa tất cả OTP codes đã hết hạn
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpCode o WHERE o.expiryDate < :currentTime")
    void deleteExpiredOtpCodes(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Đếm số OTP codes active của user
     */
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.user = :user AND o.expiryDate > :currentTime")
    long countActiveOtpsByUser(@Param("user") User user, @Param("currentTime") LocalDateTime currentTime);
}
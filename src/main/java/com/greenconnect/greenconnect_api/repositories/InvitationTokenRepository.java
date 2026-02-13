package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.greenconnect.greenconnect_api.entities.InvitationToken;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {
    
    /**
     * Tìm token mới nhất còn hiệu lực của email
     * (chưa được sử dụng, chưa hết hạn)
     */
    @Query("SELECT it FROM InvitationToken it WHERE it.email = :email " +
           "AND it.isUsed = false AND it.expiryDate > :now " +
           "ORDER BY it.createdAt DESC")
    Optional<InvitationToken> findLatestActiveByEmail(
        @Param("email") String email, 
        @Param("now") LocalDateTime now
    );
    
    /**
     * Tìm token theo hash (dùng để verify và activate)
     */
    @Query("SELECT it FROM InvitationToken it WHERE it.tokenHash = :tokenHash " +
           "AND it.isUsed = false AND it.expiryDate > :now")
    Optional<InvitationToken> findByTokenHash(
        @Param("tokenHash") String tokenHash,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Lấy tất cả token còn hiệu lực của email
     */
    @Query("SELECT it FROM InvitationToken it WHERE it.email = :email " +
           "AND it.isUsed = false AND it.expiryDate > :now " +
           "ORDER BY it.createdAt DESC")
    List<InvitationToken> findAllActiveByEmail(
        @Param("email") String email,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Đếm số token còn hiệu lực của email
     */
    @Query("SELECT COUNT(it) FROM InvitationToken it WHERE it.email = :email " +
           "AND it.isUsed = false AND it.expiryDate > :now")
    long countActiveByEmail(
        @Param("email") String email,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Xóa các token đã hết hạn (chạy định kỳ để cleanup database)
     */
    @Modifying
    @Query("DELETE FROM InvitationToken it WHERE it.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Vô hiệu hóa tất cả token của email (đánh dấu isUsed = true)
     * Dùng khi resend activation email
     */
    @Modifying
    @Query("UPDATE InvitationToken it SET it.isUsed = true " +
           "WHERE it.email = :email AND it.isUsed = false")
    int invalidateAllByEmail(@Param("email") String email);
    
    /**
     * Lấy danh sách token theo admin đã mời (phân trang)
     */
    @Query("SELECT it FROM InvitationToken it WHERE it.invitedByAdminId = :adminId " +
           "ORDER BY it.createdAt DESC")
    Page<InvitationToken> findByInvitedByAdminId(
        @Param("adminId") UUID adminId,
        Pageable pageable
    );
    
    /**
     * Kiểm tra email đã có token chưa kích hoạt hay chưa
     */
    @Query("SELECT CASE WHEN COUNT(it) > 0 THEN true ELSE false END " +
           "FROM InvitationToken it WHERE it.email = :email " +
           "AND it.isUsed = false AND it.expiryDate > :now")
    boolean existsActiveTokenByEmail(
        @Param("email") String email,
        @Param("now") LocalDateTime now
    );
}

package com.greenconnect.greenconnect_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.UserRole;
import com.greenconnect.greenconnect_api.enums.Role;

/**
 * Repository interface cho thực thể UserRole.
 * <p>Quản lý việc lưu trữ, truy vấn các assignment giữa User và Role.</p>
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Tìm tất cả active roles của một user.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findActiveRolesByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Tìm tất cả roles của một user (bao gồm cả inactive).
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Tìm primary role của user (role đầu tiên có active).
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now) ORDER BY ur.assignedAt ASC LIMIT 1")
    Optional<UserRole> findPrimaryRoleByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Tìm user-role assignment cụ thể.
     */
    Optional<UserRole> findByUserIdAndRole(UUID userId, Role role);

    /**
     * Kiểm tra user có role cụ thể không.
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user.id = :userId AND ur.role = :role AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    boolean userHasRole(@Param("userId") UUID userId, @Param("role") Role role, @Param("now") LocalDateTime now);

    /**
     * Tìm tất cả users có role cụ thể.
     */
    @Query("SELECT ur.user FROM UserRole ur WHERE ur.role = :role AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<User> findUsersByRole(@Param("role") Role role, @Param("now") LocalDateTime now);

    /**
     * Tìm tất cả users có bất kỳ role nào trong danh sách.
     */
    @Query("SELECT DISTINCT ur.user FROM UserRole ur WHERE ur.role IN :roles AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<User> findUsersByRoles(@Param("roles") Set<Role> roles, @Param("now") LocalDateTime now);

    /**
     * Đếm số lượng users có role cụ thể.
     */
    @Query("SELECT COUNT(DISTINCT ur.user) FROM UserRole ur WHERE ur.role = :role AND ur.active = true AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    long countUsersByRole(@Param("role") Role role, @Param("now") LocalDateTime now);

    /**
     * Tìm các role assignments sắp hết hạn.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt BETWEEN :startDate AND :endDate AND ur.active = true")
    List<UserRole> findExpiringRoles(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Tìm tất cả role assignments đã hết hạn.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt < :now AND ur.active = true")
    List<UserRole> findExpiredRoles(@Param("now") LocalDateTime now);

    /**
     * Xóa tất cả role assignments của user.
     */
    @Modifying
    void deleteByUserId(UUID userId);

    /**
     * Xóa tất cả role assignments của user.
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * Xóa role assignment cụ thể.
     */
    @Modifying
    void deleteByUserIdAndRole(UUID userId, Role role);
    
    /**
     * Kiểm tra user có role cụ thể không.
     */
    boolean existsByUserIdAndRole(UUID userId, Role role);
    
    /**
     * Kiểm tra có tồn tại user nào có role cụ thể không.
     */
    boolean existsByRole(Role role);
    
    /**
     * Tìm tất cả UserRole của một user, sắp xếp theo thời gian assign.
     */
    List<UserRole> findByUserIdOrderByAssignedAtDesc(UUID userId);
}
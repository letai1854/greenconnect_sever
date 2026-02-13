package com.greenconnect.greenconnect_api.utils;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;

/**
 * Security Utils - CHỨC NĂNG MỚI
 * 
 * Chức năng: Helper methods để lấy thông tin user hiện tại trong controllers
 * - Lấy user từ SecurityContext (đã set bởi JwtAuthenticationFilter)
 * - Kiểm tra roles và permissions
 * - Sử dụng trong controllers để validate quyền truy cập
 */
@Component
public class SecurityUtils {

    /**
     * Lấy user hiện tại từ Security Context
     */
    public static CustomUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        if (authentication.getPrincipal() instanceof CustomUserPrincipal) {
            return (CustomUserPrincipal) authentication.getPrincipal();
        }
        
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * Lấy User ID hiện tại
     */
    public static UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * Lấy email hiện tại
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    /**
     * Kiểm tra user có role không - SỬ DỤNG TRONG @PreAuthorize
     */
    public static boolean hasRole(Role role) {
        return getCurrentUser().hasRole(role);
    }

    /**
     * Kiểm tra user có ít nhất 1 trong các roles không
     */
    public static boolean hasAnyRole(Role... roles) {
        return getCurrentUser().hasAnyRole(roles);
    }

    /**
     * Kiểm tra xem có phải admin không
     */
    public static boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /**
     * Kiểm tra xem có phải chính user đó không (own-resource access)
     */
    public static boolean isOwnerOrAdmin(UUID resourceUserId) {
        UUID currentUserId = getCurrentUserId();
        return currentUserId.equals(resourceUserId) || isAdmin();
    }
}
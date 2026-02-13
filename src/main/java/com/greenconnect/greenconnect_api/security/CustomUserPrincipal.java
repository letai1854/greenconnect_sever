package com.greenconnect.greenconnect_api.security;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Custom User Principal - CHỨC NĂNG MỚI
 * 
 * Chức năng: Lưu trữ thông tin user đã authenticate trong Security Context
 * - Chứa userId, email, và tất cả roles
 * - Cung cấp helper methods để kiểm tra quyền
 * - Được sử dụng bởi @PreAuthorize và SecurityUtils
 */
@Getter
@AllArgsConstructor
public class CustomUserPrincipal implements Principal {
    
    private final UUID userId;
    private final String email;
    private final Set<Role> roles; // MULTI-ROLE từ JWT scope
    
    @Override
    public String getName() {
        return email;
    }
    
    /**
     * Kiểm tra user có role cụ thể không
     * Sử dụng trong @PreAuthorize
     */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
    
    /**
     * Kiểm tra user có ít nhất 1 trong các roles không
     */
    public boolean hasAnyRole(Role... rolesArray) {
        for (Role role : rolesArray) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra có phải admin không
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}
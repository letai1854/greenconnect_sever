package com.greenconnect.greenconnect_api.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * EnumMapper - Mapper giữa tiếng Việt và tiếng Anh cho các enum
 * <p>Dùng để convert yêu cầu từ frontend (tiếng Việt) sang backend enums (tiếng Anh)</p>
 */
@Slf4j
public class EnumMapper {
    
    // ==================== ROLE MAPPER ====================
    
    /**
     * Map tiếng Việt → Role enum (tiếng Anh)
     */
    private static final Map<String, Role> VIETNAMESE_TO_ROLE = new HashMap<>();
    
    static {
        // Khách hàng
        VIETNAMESE_TO_ROLE.put("khách hàng", Role.CUSTOMER);
        
        // Admin
        VIETNAMESE_TO_ROLE.put("admin", Role.ADMIN);
        
        // Staff roles
        VIETNAMESE_TO_ROLE.put("quản lý đơn hàng", Role.ORDER_MANAGER);
        VIETNAMESE_TO_ROLE.put("quản lý sản phẩm", Role.PRODUCT_MANAGER);
        VIETNAMESE_TO_ROLE.put("quản lý marketing", Role.MARKETING_MANAGER);
        VIETNAMESE_TO_ROLE.put("hỗ trợ khách hàng", Role.CUSTOMER_SUPPORT);
        VIETNAMESE_TO_ROLE.put("nhân viên giao hàng", Role.SHIPPER);
    }
    
    /**
     * Map Role enum (tiếng Anh) → tiếng Việt
     */
    private static final Map<Role, String> ROLE_TO_VIETNAMESE = new HashMap<>();
    
    static {
        ROLE_TO_VIETNAMESE.put(Role.CUSTOMER, "khách hàng");
        ROLE_TO_VIETNAMESE.put(Role.ADMIN, "admin");
        ROLE_TO_VIETNAMESE.put(Role.ORDER_MANAGER, "quản lý đơn hàng");
        ROLE_TO_VIETNAMESE.put(Role.PRODUCT_MANAGER, "quản lý sản phẩm");
        ROLE_TO_VIETNAMESE.put(Role.MARKETING_MANAGER, "quản lý marketing");
        ROLE_TO_VIETNAMESE.put(Role.CUSTOMER_SUPPORT, "hỗ trợ khách hàng");
        ROLE_TO_VIETNAMESE.put(Role.SHIPPER, "nhân viên giao hàng");
    }
    
    // ==================== USER STATUS MAPPER ====================
    
    /**
     * Map tiếng Việt → UserStatus enum (tiếng Anh)
     */
    private static final Map<String, UserStatus> VIETNAMESE_TO_STATUS = new HashMap<>();
    
    static {
        VIETNAMESE_TO_STATUS.put("hoạt động", UserStatus.ACTIVE);
        VIETNAMESE_TO_STATUS.put("không hoạt động", UserStatus.INACTIVE);
        VIETNAMESE_TO_STATUS.put("đang chờ kích hoạt", UserStatus.PENDING_ACTIVATION);
    }
    
    /**
     * Map UserStatus enum (tiếng Anh) → tiếng Việt
     */
    private static final Map<UserStatus, String> STATUS_TO_VIETNAMESE = new HashMap<>();
    
    static {
        STATUS_TO_VIETNAMESE.put(UserStatus.ACTIVE, "hoạt động");
        STATUS_TO_VIETNAMESE.put(UserStatus.INACTIVE, "không hoạt động");
        STATUS_TO_VIETNAMESE.put(UserStatus.PENDING_ACTIVATION, "đang chờ kích hoạt");
    }
    
    // ==================== PUBLIC METHODS - ROLE ====================
    
    /**
     * Convert tiếng Việt sang Role enum
     * <p>Không phân biệt chữ hoa/thường</p>
     * 
     * @param vietnameseName tên role tiếng Việt (vd: "quản lý đơn hàng")
     * @return Role enum, hoặc null nếu không tìm thấy
     */
    public static Role toRole(String vietnameseName) {
        if (vietnameseName == null || vietnameseName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = vietnameseName.trim().toLowerCase();
        Role role = VIETNAMESE_TO_ROLE.get(normalized);
        
        if (role == null) {
            log.warn("⚠️ [ENUM MAPPER] Không tìm thấy role tiếng Việt: '{}'", vietnameseName);
        }
        
        return role;
    }
    
    /**
     * Convert danh sách tiếng Việt sang Set<Role>
     * <p>Bỏ qua các giá trị không hợp lệ</p>
     * 
     * @param vietnameseNames danh sách tên role tiếng Việt
     * @return Set<Role> chứa các role hợp lệ
     */
    public static Set<Role> toRoles(Set<String> vietnameseNames) {
        Set<Role> roles = new HashSet<>();
        
        if (vietnameseNames == null || vietnameseNames.isEmpty()) {
            return roles;
        }
        
        for (String name : vietnameseNames) {
            Role role = toRole(name);
            if (role != null) {
                roles.add(role);
                log.debug("✅ Converted role: '{}' → {}", name, role);
            }
        }
        
        return roles;
    }
    
    /**
     * Convert Role enum sang tiếng Việt
     * 
     * @param role enum tiếng Anh
     * @return tên role tiếng Việt
     */
    public static String toVietnameseName(Role role) {
        if (role == null) {
            return "không xác định";
        }
        
        return ROLE_TO_VIETNAMESE.getOrDefault(role, role.name());
    }
    
    /**
     * Convert Set<Role> sang danh sách tiếng Việt
     * 
     * @param roles Set<Role> enum
     * @return danh sách tên role tiếng Việt
     */
    public static Set<String> toVietnameseNames(Set<Role> roles) {
        Set<String> names = new HashSet<>();
        
        if (roles == null || roles.isEmpty()) {
            return names;
        }
        
        for (Role role : roles) {
            names.add(toVietnameseName(role));
        }
        
        return names;
    }
    
    // ==================== PUBLIC METHODS - USER STATUS ====================
    
    /**
     * Convert tiếng Việt sang UserStatus enum
     * <p>Không phân biệt chữ hoa/thường</p>
     * 
     * @param vietnameseName tên status tiếng Việt (vd: "hoạt động")
     * @return UserStatus enum, hoặc null nếu không tìm thấy
     */
    public static UserStatus toUserStatus(String vietnameseName) {
        if (vietnameseName == null || vietnameseName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = vietnameseName.trim().toLowerCase();
        UserStatus status = VIETNAMESE_TO_STATUS.get(normalized);
        
        if (status == null) {
            log.warn("⚠️ [ENUM MAPPER] Không tìm thấy status tiếng Việt: '{}'", vietnameseName);
        }
        
        return status;
    }
    
    /**
     * Convert UserStatus enum sang tiếng Việt
     * 
     * @param status enum tiếng Anh
     * @return tên status tiếng Việt
     */
    public static String toVietnameseName(UserStatus status) {
        if (status == null) {
            return "không xác định";
        }
        
        return STATUS_TO_VIETNAMESE.getOrDefault(status, status.name());
    }
    
    // ==================== SEARCH FIELD MAPPER ====================
    
    /**
     * Convert tên trường tìm kiếm tiếng Việt sang tiếng Anh
     * <p>Hỗ trợ: "tên", "email", "điện thoại"</p>
     * 
     * @param vietnameseField tên trường tiếng Việt
     * @return tên trường tiếng Anh (database field name), hoặc null nếu không hợp lệ
     */
    public static String toSearchField(String vietnameseField) {
        if (vietnameseField == null || vietnameseField.trim().isEmpty()) {
            return null;
        }
        
        String normalized = vietnameseField.trim().toLowerCase();
        
        return switch (normalized) {
            case "tên" -> "fullName";
            case "email" -> "email";
            case "điện thoại" -> "phoneNumber";
            default -> {
                log.warn("⚠️ [ENUM MAPPER] Không tìm thấy search field: '{}'", vietnameseField);
                yield null;
            }
        };
    }
}

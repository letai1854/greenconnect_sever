package com.greenconnect.greenconnect_api.controllers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.UserRole;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.repositories.UserRoleRepository;
import com.greenconnect.greenconnect_api.utils.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Test Controller để kiểm tra multi-role system.
 * <p>CHỈ SỬ DỤNG TRONG DEVELOPMENT/TESTING</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtUtils jwtUtils;
    
    /**
     * Test 1: Kiểm tra JWT token có chứa scope với multiple roles
     */
    @GetMapping("/jwt-decode")
    public ResponseEntity<?> testJwtDecode(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", jwtUtils.getUserIdFromToken(token));
            result.put("email", jwtUtils.getEmailFromToken(token));
            result.put("primaryRole", jwtUtils.getRoleFromToken(token));
            result.put("scope", jwtUtils.getScopeFromToken(token));
            result.put("allRoles", jwtUtils.getRolesFromToken(token));
            result.put("fullName", jwtUtils.getFullNameFromToken(token));
            
            // Xử lý riêng isTokenValid vì có thể throw ExpiredJwtException
            try {
                result.put("isValid", jwtUtils.isTokenValid(token));
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                result.put("isValid", false);
                result.put("expired", true);
                result.put("expiredAt", e.getClaims().getExpiration());
            }
            
            return ResponseEntity.ok(result);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Token expired: ", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "TOKEN_EXPIRED");
            errorResult.put("message", e.getMessage());
            return ResponseEntity.status(409).body(errorResult);
        } catch (Exception e) {
            log.error("Error decoding JWT: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 2: Thêm role cho user (để test multiple roles)
     */
    @PostMapping("/users/{userId}/roles/{role}")
    public ResponseEntity<?> addRoleToUser(@PathVariable UUID userId, @PathVariable String role) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Role roleEnum = Role.valueOf(role.toUpperCase());
            
            // Kiểm tra role đã tồn tại chưa
            if (userRoleRepository.existsByUserIdAndRole(userId, roleEnum)) {
                return ResponseEntity.badRequest().body("User already has this role");
            }
            
            // Thêm role mới
            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(roleEnum)
                    .assignedAt(LocalDateTime.now())
                    .build();
            
            userRoleRepository.save(userRole);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Role added successfully");
            result.put("userId", userId);
            result.put("roleAdded", role);
            result.put("currentRoles", user.getActiveRoles());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error adding role: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 3: Xem tất cả roles của một user
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<UserRole> userRoles = userRoleRepository.findByUserIdOrderByAssignedAtDesc(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("email", user.getEmail());
            result.put("activeRoles", user.getActiveRoles());
            result.put("primaryRole", user.getPrimaryRole());
            result.put("allUserRoles", userRoles.stream().map(ur -> {
                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("role", ur.getRole());
                roleInfo.put("assignedAt", ur.getAssignedAt());
                roleInfo.put("expiresAt", ur.getExpiresAt());
                roleInfo.put("isValid", ur.isValid());
                return roleInfo;
            }).toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting user roles: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 4: Tạo một user test với multiple roles
     */
    @PostMapping("/create-test-user")
    public ResponseEntity<?> createTestUser(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String fullName = request.get("fullName");
            String rolesStr = request.get("roles"); // "CUSTOMER,ADMIN,SHIPPER"
            
            if (userRepository.existsByEmailIgnoreCase(email)) {
                return ResponseEntity.badRequest().body("Email already exists");
            }
            
            // Tạo user
            User user = User.builder()
                    .email(email.toLowerCase())
                    .fullName(fullName)
                    .passwordHash("$2a$10$dummy.hash.for.testing") // Dummy hash
                    .provider(com.greenconnect.greenconnect_api.enums.Provider.LOCAL)
                    .status(com.greenconnect.greenconnect_api.enums.UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            User savedUser = userRepository.save(user);
            
            // Thêm roles
            String[] roles = rolesStr.split(",");
            boolean isFirst = true;
            
            for (String roleStr : roles) {
                Role role = Role.valueOf(roleStr.trim().toUpperCase());
                
                UserRole userRole = UserRole.builder()
                        .user(savedUser)
                        .role(role)
                        .assignedAt(LocalDateTime.now())
                        .build();
                
                userRoleRepository.save(userRole);
                isFirst = false;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Test user created successfully");
            result.put("userId", savedUser.getId());
            result.put("email", savedUser.getEmail());
            
            // Refresh user để load UserRole relationships
            User refreshedUser = userRepository.findById(savedUser.getId()).orElse(savedUser);
            result.put("roles", refreshedUser.getActiveRoles());
            result.put("primaryRole", refreshedUser.getPrimaryRole());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating test user: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 5: Tạo JWT token mới cho user (để test scope)
     */
    @PostMapping("/users/{userId}/generate-token")
    public ResponseEntity<?> generateTestToken(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Set<Role> activeRoles = user.getActiveRoles();
            Role primaryRole = user.getPrimaryRole();
            
            if (primaryRole == null) {
                return ResponseEntity.badRequest().body("User has no primary role");
            }
            
            // Tạo access token với multiple roles
            String accessToken = jwtUtils.generateAccessToken(
                    user.getId(), 
                    user.getEmail(), 
                    activeRoles, 
                    primaryRole, 
                    user.getFullName()
            );
            
            // Tạo refresh token với primary role
            String refreshToken = jwtUtils.generateRefreshToken(
                    user.getId(), 
                    user.getEmail(), 
                    primaryRole.name(), 
                    user.getFullName()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("tokenType", "Bearer");
            result.put("userId", userId);
            result.put("activeRoles", activeRoles);
            result.put("primaryRole", primaryRole);
            
            // Decode để kiểm tra
            result.put("decodedToken", Map.of(
                "scope", jwtUtils.getScopeFromToken(accessToken),
                "role", jwtUtils.getRoleFromToken(accessToken),
                "roles", jwtUtils.getRolesFromToken(accessToken)
            ));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating test token: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 6: Lấy danh sách tất cả users và roles của họ
     */
    @GetMapping("/users-roles-summary")
    public ResponseEntity<?> getUsersRolesSummary() {
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> result = users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getId());
                userInfo.put("email", user.getEmail());
                userInfo.put("fullName", user.getFullName());
                userInfo.put("activeRoles", user.getActiveRoles());
                userInfo.put("primaryRole", user.getPrimaryRole());
                userInfo.put("provider", user.getProvider());
                userInfo.put("status", user.getStatus());
                return userInfo;
            }).toList();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting users summary: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Test 7: Debug - Refresh user and check roles
     */
    @GetMapping("/users/{userId}/debug")
    public ResponseEntity<?> debugUserRoles(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<UserRole> userRoles = userRoleRepository.findByUserIdOrderByAssignedAtDesc(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("email", user.getEmail());
            result.put("userRolesFromRepo", userRoles.size());
            result.put("activeRolesFromEntity", user.getActiveRoles());
            result.put("primaryRoleFromEntity", user.getPrimaryRole());
            result.put("userRoleDetails", userRoles.stream().map(ur -> {
                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("id", ur.getId());
                roleInfo.put("role", ur.getRole());
                roleInfo.put("assignedAt", ur.getAssignedAt());
                roleInfo.put("expiresAt", ur.getExpiresAt());
                roleInfo.put("isValid", ur.isValid());
                return roleInfo;
            }).toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error debugging user roles: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
package com.greenconnect.greenconnect_api.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.enums.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Utility class for token generation and validation.
 * <p>Xử lý tạo, validate và extract thông tin từ JWT tokens cho User authentication.</p>
 */
@Slf4j
@Component
public class JwtUtils {
    
    @Value("${jwt.signerkey}")
    protected String SIGNER_KEY;
    
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    /**
     * Tạo JWT access token cho user với multiple roles.
     * <p>Chứa thông tin: userId, email, roles (as scope), primaryRole, fullName từ User entity.</p>
     *
     * @param userId User ID từ User.id
     * @param email Email từ User.email
     * @param roles All active roles từ User.getActiveRoles()
     * @param primaryRole Primary role từ User.getPrimaryRole()
     * @param fullName Full name từ User.fullName
     * @return JWT access token string
     */
    public String generateAccessToken(UUID userId, String email, Set<Role> roles, Role primaryRole, String fullName) {
        String scope = roles.stream()
                           .map(Role::name)
                           .collect(Collectors.joining(" "));
        return generateTokenWithScope(userId, email, scope, primaryRole.name(), fullName, accessTokenExpiration);
    }
    
    /**
     * Tạo JWT access token cho user (backward compatibility).
     * <p>Chứa thông tin: userId, email, primaryRole, fullName từ User entity.</p>
     *
     * @param userId User ID từ User.id
     * @param email Email từ User.email
     * @param role Primary role từ User.getPrimaryRole()
     * @param fullName Full name từ User.fullName
     * @return JWT access token string
     */
    public String generateAccessToken(UUID userId, String email, String role, String fullName) {
        return generateToken(userId, email, role, fullName, accessTokenExpiration);
    }
    
    /**
     * Tạo refresh token cho user.
     * <p>Chứa thông tin tương tự access token nhưng expire lâu hơn (7 ngày).</p>
     * <p>Role được lưu là primary role của user.</p>
     */
    public String generateRefreshToken(UUID userId, String email, String role, String fullName) {
        return generateToken(userId, email, role, fullName, refreshTokenExpiration);
    }
    
    
    /**
     * Tạo JWT token với thời gian hết hạn cụ thể.
     * <p>Token chứa claims từ User entity và RefreshToken metadata.</p>
     * <p>Role claim chứa primary role của user.</p>
     */
    private String generateToken(UUID userId, String email, String role, String fullName, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        return Jwts.builder()
                .setSubject(userId.toString())              // User.id
                .claim("email", email)                      // User.email
                .claim("role", role)                        // User.getPrimaryRole()
                .claim("fullName", fullName)                // User.fullName
                .claim("tokenType", "access")               // Token type
                .setIssuedAt(now)                          // RefreshToken.createdAt equivalent
                .setExpiration(expiryDate)                 // RefreshToken.expiryDate equivalent
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Tạo JWT token với scope chứa multiple roles.
     * <p>Token chứa scope/roles claim với tất cả roles và role claim với primary role.</p>
     * <p>scope và roles đều là space-separated string chứa tất cả roles.</p>
     */
    private String generateTokenWithScope(UUID userId, String email, String scope, String primaryRole, String fullName, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        return Jwts.builder()
                .setSubject(userId.toString())              // User.id
                .claim("email", email)                      // User.email
                .claim("scope", scope)                      // All roles separated by space
                .claim("roles", scope)                      // All roles separated by space (same as scope)
                .claim("role", primaryRole)                 // Primary role
                .claim("fullName", fullName)                // User.fullName
                .claim("tokenType", "access")               // Token type
                .setIssuedAt(now)                          // RefreshToken.createdAt equivalent
                .setExpiration(expiryDate)                 // RefreshToken.expiryDate equivalent
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Extract User ID từ JWT token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }
    
    /**
     * Extract email từ JWT token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }
    
    /**
     * Extract role từ JWT token.
     * <p>Trả về primary role của user.</p>
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * Extract scope từ JWT token.
     * <p>Trả về tất cả roles dưới dạng space-separated string.</p>
     */
    public String getScopeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("scope", String.class);
    }
    
    /**
     * Extract roles từ scope claim trong JWT token.
     * <p>Chuyển đổi scope string thành Set của Role enum.</p>
     */
    public Set<Role> getRolesFromToken(String token) {
        String scope = getScopeFromToken(token);
        if (scope == null || scope.trim().isEmpty()) {
            // Fallback to single role for backward compatibility
            String singleRole = getRoleFromToken(token);
            return singleRole != null ? Set.of(Role.valueOf(singleRole)) : Set.of();
        }
        
        return Set.of(scope.split(" "))
                  .stream()
                  .map(Role::valueOf)
                  .collect(Collectors.toSet());
    }
    
    /**
     * Extract roles từ "roles" claim trong JWT token.
     * <p>Trả về danh sách các role từ claim "roles" dưới dạng space-separated string.</p>
     */
    public String getRolesStringFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roles", String.class);
    }
    
    /**
     * Extract full name từ JWT token.
     */
    public String getFullNameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("fullName", String.class);
    }
    
    /**
     * Lấy thời gian hết hạn của token.
     */
    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * Validate JWT token.
     * <p>Kiểm tra signature, expiration, và format.</p>
     * <p>⚠️ QUAN TRỌNG: Throw ExpiredJwtException ra ngoài để JwtAuthenticationFilter xử lý riêng!</p>
     * 
     * @throws ExpiredJwtException khi token đã hết hạn (để trả về 4423)
     */
    public boolean validateToken(String token) throws ExpiredJwtException {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (ExpiredJwtException ex) {
            // ⭐ THROW RA NGOÀI để JwtAuthenticationFilter bắt và trả về 4423 ACCESS_TOKEN_EXPIRED
            log.warn("⏰ Expired JWT token: {}", ex.getMessage());
            throw ex;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Alias for validateToken method.
     * <p>Kiểm tra token có hợp lệ không.</p>
     * 
     * @throws ExpiredJwtException khi token đã hết hạn (để trả về 4423)
     */
    public boolean isTokenValid(String token) throws ExpiredJwtException {
        return validateToken(token);
    }
    
    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Extract claims từ JWT token.
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Tạo signing key từ SIGNER_KEY.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SIGNER_KEY.getBytes());
    }
    
    /**
     * Get access token expiration time in seconds.
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    /**
     * Get refresh token expiration time in seconds.
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
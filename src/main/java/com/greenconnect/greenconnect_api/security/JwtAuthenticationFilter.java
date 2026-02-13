package com.greenconnect.greenconnect_api.security;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter - CHỨC NĂNG MỚI
 * 
 * Chức năng: Tự động xác thực user từ JWT token trong mỗi request
 * - Extract token từ Authorization header
 * - Validate token và extract user info
 * - Set Authentication vào SecurityContext để @PreAuthorize hoạt động
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils; // SỬ DỤNG JWT UTILS CÓ SẴN
    
    @Autowired
    private ObjectMapper objectMapper; // Để serialize ApiResponse

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        // ========== WHITELIST VNPAY CALLBACK (KHÔNG CẦN JWT) ==========
        String requestPath = request.getRequestURI();
        if (requestPath.equals("/orders/vnpay-callback")) {
            log.debug("🔓 [JWT FILTER] Bypassing JWT check for VNPay callback");
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = extractTokenFromRequest(request);
        
        // ========== SKIP NẾU KHÔNG CÓ TOKEN ==========
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // ========== VALIDATE TOKEN ==========
            // ⚠️ isTokenValid() sẽ throw ExpiredJwtException nếu token hết hạn
            if (!jwtUtils.isTokenValid(token)) {
                log.warn("❌ [JWT FILTER] Token không hợp lệ");
                sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
                return;
            }
            
            // ========== EXTRACT THÔNG TIN USER ==========
            UUID userId = jwtUtils.getUserIdFromToken(token);
            String email = jwtUtils.getEmailFromToken(token);
            Set<Role> roles = jwtUtils.getRolesFromToken(token); // MULTI-ROLE từ scope
            
            // ========== CHUYỂN ĐỔI ROLES thành Spring Security authorities ==========
            // Thêm prefix "ROLE_" để @PreAuthorize('hasRole(ADMIN)') hoạt động
            Set<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet());
            
            // ========== TẠO CUSTOM USER PRINCIPAL ==========
            CustomUserPrincipal userPrincipal = new CustomUserPrincipal(userId, email, roles);
            
            // ========== TẠO AUTHENTICATION OBJECT ==========
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // ========== SET VÀO SECURITY CONTEXT ==========
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("✅ [JWT FILTER] Authenticated user: {} với roles: {}", email, roles);
            
            // ========== CONTINUE FILTER CHAIN ==========
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            // ⭐ BẮT RIÊNG ExpiredJwtException → 409 CONFLICT
            log.warn("⏰ [JWT FILTER] Access token đã hết hạn: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, ErrorCode.ACCESS_TOKEN_EXPIRED);
            
        } catch (Exception e) {
            // ⚠️ CÁC LỖI KHÁC (malformed, invalid signature, etc.) → 401 UNAUTHORIZED
            log.error("❌ [JWT FILTER] Token validation failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * Extract Bearer token từ Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * Gửi error response về client với format ApiResponse chuẩn.
     * <p>Sử dụng khi token expired hoặc invalid trong filter</p>
     */
    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .data(null)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
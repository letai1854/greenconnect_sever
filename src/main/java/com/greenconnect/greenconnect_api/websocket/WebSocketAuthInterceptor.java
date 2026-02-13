package com.greenconnect.greenconnect_api.websocket;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.utils.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor // Sử dụng constructor injection
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    @Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
        log.info("🔌 [WebSocket Auth] CONNECT attempt - Session: {}", accessor.getSessionId());
        
        String authToken = accessor.getFirstNativeHeader("Authorization");
        log.info("📝 [WebSocket Auth] Token status: {}", authToken != null ? "Present" : "❌ MISSING");
        
        if (authToken != null && authToken.startsWith("Bearer ")) {
            String jwt = authToken.substring(7);
            log.debug("🔑 [WebSocket Auth] Token preview: {}...", jwt.substring(0, Math.min(20, jwt.length())));
            
            try {
                log.info("🔐 [WebSocket Auth] Validating JWT token...");
                
                if (jwtUtils.isTokenValid(jwt)) {
                    log.info("✅ [WebSocket Auth] Token validation PASSED");
                    
                    UUID userId = jwtUtils.getUserIdFromToken(jwt);
                    String email = jwtUtils.getEmailFromToken(jwt);
                    Set<Role> roles = jwtUtils.getRolesFromToken(jwt);
                    
                    log.info("👤 [WebSocket Auth] Authenticated user - ID: {}, Email: {}, Roles: {}", 
                            userId, email, roles);
                    
                    // Chuyển đổi Roles thành Authorities
                    Set<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toSet());
                    
                    // Tạo CustomUserPrincipal
                    CustomUserPrincipal userPrincipal = new CustomUserPrincipal(userId, email, roles);

                    // Tạo Authentication
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userPrincipal, null, authorities);

                    // Đặt vào session
                    accessor.setUser(authentication);
                    log.info("✅ [WebSocket Auth] SUCCESS - User {} authenticated", email);
                    
                } else {
                    // Token không hợp lệ (hết hạn hoặc invalid)
                    String errorMsg = "TOKEN_EXPIRED_OR_INVALID";
                    log.error("❌ [WebSocket Auth] FAILED - {}", errorMsg);
                    log.error("💡 [WebSocket Auth] Client should refresh token and reconnect");
                    
                    // Gửi ERROR frame với message rõ ràng để client biết cần refresh token
                    accessor.setHeader("message", errorMsg);
                    return createErrorFrame(accessor, errorMsg);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                // Token đã hết hạn - trường hợp cụ thể nhất
                String errorMsg = "TOKEN_EXPIRED";
                log.error("⏰ [WebSocket Auth] Token EXPIRED - Issued at: {}, Expired at: {}", 
                         e.getClaims().getIssuedAt(), e.getClaims().getExpiration());
                log.info("💡 [WebSocket Auth] Client should refresh token and reconnect");
                
                accessor.setHeader("message", errorMsg);
                return createErrorFrame(accessor, errorMsg);
                
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                String errorMsg = "TOKEN_MALFORMED";
                log.error("🔧 [WebSocket Auth] Token MALFORMED: {}", e.getMessage());
                
                accessor.setHeader("message", errorMsg);
                return createErrorFrame(accessor, errorMsg);
                
            } catch (Exception e) {
                String errorMsg = "AUTH_FAILED: " + e.getMessage();
                log.error("❌ [WebSocket Auth] Unexpected error: {}", e.getMessage(), e);
                
                accessor.setHeader("message", errorMsg);
                return createErrorFrame(accessor, errorMsg);
            }
        } else {
            String errorMsg = "MISSING_TOKEN";
            log.error("❌ [WebSocket Auth] No Authorization header or invalid format");
            
            accessor.setHeader("message", errorMsg);
            return createErrorFrame(accessor, errorMsg);
        }
    }
    return message;
}

/**
 * Tạo STOMP ERROR frame để gửi về client
 * Client sẽ nhận được message này trong callback onStompError
 */
private Message<?> createErrorFrame(StompHeaderAccessor accessor, String errorMessage) {
    StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
    errorAccessor.setSessionId(accessor.getSessionId());
    errorAccessor.setMessage(errorMessage);
    errorAccessor.setLeaveMutable(true);
    
    log.info("📤 [WebSocket Auth] Sending ERROR frame to client: {}", errorMessage);
    
    return org.springframework.messaging.support.MessageBuilder
            .createMessage(new byte[0], errorAccessor.getMessageHeaders());
}
}
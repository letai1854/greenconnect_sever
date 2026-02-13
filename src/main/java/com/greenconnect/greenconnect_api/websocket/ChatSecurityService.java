package com.greenconnect.greenconnect_api.websocket;

import com.greenconnect.greenconnect_api.entities.Conversation;
import com.greenconnect.greenconnect_api.repositories.ConversationRepository;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Service("chatSecurityService") // Đặt tên cho bean để gọi trong SpEL
@RequiredArgsConstructor
public class ChatSecurityService {

    private final ConversationRepository conversationRepository;

    public boolean canAccessConversation(UUID conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        
        // Admin và nhân viên hỗ trợ có toàn quyền truy cập
        if (principal.hasRole(com.greenconnect.greenconnect_api.enums.Role.ADMIN) || 
            principal.hasRole(com.greenconnect.greenconnect_api.enums.Role.CUSTOMER_SUPPORT)) {
            return true;
        }
        
        // Khách hàng chỉ được truy cập vào cuộc trò chuyện của chính họ
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return false; // Hoặc ném exception tùy logic
        }
        
        // Kiểm tra xem ID của khách hàng trong conversation có khớp với ID của người đang đăng nhập không
        return conversation.getCustomer().getId().equals(principal.getUserId());
    }
}
package com.greenconnect.greenconnect_api.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    private String userQuery;              // Câu hỏi hiện tại của user
    
    // ❌ ĐÃ XÓA: userId, sessionId, includeProductIds, excludeProductIds, history
    // ✅ LÝ DO: 
    // - userId: Lấy từ JWT token (SecurityContext)
    // - sessionId: Backend tự tìm session active của user
    // - includeProductIds/excludeProductIds: Backend tự động load từ DB
    // - history: Backend tự động load 10 tin nhắn gần nhất
    
    // 🎯 FRONTEND CHỈ CẦN GỬI: userQuery
}

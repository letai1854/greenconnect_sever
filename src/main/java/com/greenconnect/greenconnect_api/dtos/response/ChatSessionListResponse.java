package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response cho API lấy danh sách session của user
 * Để hiển thị sidebar "Chat history" (giống ChatGPT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionListResponse {
    
    private List<SessionItem> sessions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionItem {
        private UUID id;
        private String title;           // VD: "Hỏi về thịt gà"
        private LocalDateTime updatedAt;
        private int messageCount;       // Số tin nhắn trong session
        private String lastMessage;     // Preview tin nhắn cuối cùng
    }
}

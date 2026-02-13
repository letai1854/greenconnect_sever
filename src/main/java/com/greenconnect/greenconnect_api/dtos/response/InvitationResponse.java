package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin lời mời
 * Dùng để phản hồi sau khi gửi lời mời hoặc resend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {
    
    private String email;
    private String fullName;
    private String role;
    private boolean tokenSent;
    private LocalDateTime expiryDate;
    private String message;
    
    // Thông tin để admin theo dõi
    private LocalDateTime invitedAt;
    private String invitedByAdmin;
}

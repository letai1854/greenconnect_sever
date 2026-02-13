package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;

import com.greenconnect.greenconnect_api.enums.UserStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO sau khi cập nhật trạng thái tài khoản user.
 * <p>Chứa thông tin user và trạng thái mới sau khi cập nhật</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserStatusResponse {
    
    /**
     * Thông tin user đã được cập nhật.
     */
    UserResponse user;
    
    /**
     * Trạng thái cũ trước khi cập nhật.
     */
    UserStatus previousStatus;
    
    /**
     * Trạng thái mới sau khi cập nhật.
     */
    UserStatus newStatus;
    
    /**
     * Lý do thay đổi trạng thái.
     */
    String reason;
    
    /**
     * Thời gian thực hiện thay đổi.
     */
    LocalDateTime updatedAt;
    
    /**
     * Email của admin/support thực hiện thay đổi.
     */
    String updatedBy;
}

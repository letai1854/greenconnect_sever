package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho refresh token endpoint.
 * <p>Chứa refresh token để tạo access token mới.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
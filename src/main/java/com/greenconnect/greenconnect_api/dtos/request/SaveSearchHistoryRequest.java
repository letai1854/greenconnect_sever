package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO request lưu lịch sử tìm kiếm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveSearchHistoryRequest {
    
    /**
     * Nội dung tìm kiếm (keyword)
     */
    @NotBlank(message = "Nội dung tìm kiếm không được để trống")
    @Size(max = 500, message = "Nội dung tìm kiếm không được vượt quá 500 ký tự")
    private String searchKeyword;
}

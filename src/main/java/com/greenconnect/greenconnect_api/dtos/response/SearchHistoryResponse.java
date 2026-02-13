package com.greenconnect.greenconnect_api.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response lịch sử tìm kiếm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryResponse {
    
    /**
     * ID lịch sử tìm kiếm
     */
    private UUID id;
    
    /**
     * Nội dung tìm kiếm
     */
    private String searchKeyword;
    
    /**
     * Thời gian tìm kiếm
     */
    private LocalDateTime createdAt;
}

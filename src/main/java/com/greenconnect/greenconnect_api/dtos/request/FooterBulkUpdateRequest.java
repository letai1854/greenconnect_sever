package com.greenconnect.greenconnect_api.dtos.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FooterBulkUpdateRequest {
    
    @NotNull(message = "Danh sách sections là bắt buộc")
    @Valid
    private List<BulkSectionData> sections;
    
    private List<UUID> deletedSectionIds;
    
    private List<UUID> deletedLinkIds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkSectionData {
        // ⚠️ FRONTEND: GỬI null khi tạo mới, GỬI UUID (từ response) khi cập nhật
        // ❌ KHÔNG gửi createdAt, updatedAt, hoặc bất kỳ field nào khác vào đây!
        private UUID id;
        
        @NotNull(message = "Tên nhóm là bắt buộc")
        private String name;
        
        @NotNull(message = "Thứ tự là bắt buộc")
        private Integer sortOrder;
        
        private Boolean isActive;
        
        @Valid
        private List<BulkLinkData> links;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BulkLinkData {
        // ⚠️ FRONTEND: GỬI null khi tạo mới, GỬI UUID (từ response) khi cập nhật
        // ❌ KHÔNG gửi createdAt, updatedAt, hoặc bất kỳ field nào khác vào đây!
        private UUID id;
        
        @NotNull(message = "icon_key là bắt buộc")
        private String iconKey;
        
        @NotNull(message = "Giá trị là bắt buộc")
        private String value;
        
        private Boolean isActive;
    }
}

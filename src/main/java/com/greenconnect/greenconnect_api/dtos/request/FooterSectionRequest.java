package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FooterSectionRequest {
    
    @NotBlank(message = "Tên nhóm là bắt buộc")
    @Size(max = 255, message = "Tên nhóm không được vượt quá 255 ký tự")
    private String name;
    
    @NotNull(message = "Thứ tự là bắt buộc")
    @Min(value = 1, message = "Thứ tự phải là số nguyên dương")
    private Integer sortOrder;
    
    private Boolean isActive;
}

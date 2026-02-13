package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateShopReplyRequest {
    @NotNull(message = "Nội dung phản hồi không được null")
    @Size(min = 1, max = 1000, message = "Nội dung phải từ 1-1000 ký tự")
    private String content;
}
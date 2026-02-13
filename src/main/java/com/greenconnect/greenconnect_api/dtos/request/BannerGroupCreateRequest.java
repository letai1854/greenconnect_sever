package com.greenconnect.greenconnect_api.dtos.request;

import com.greenconnect.greenconnect_api.enums.BannerGroupLayoutType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Data
public class BannerGroupCreateRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    private String groupName;

    @NotBlank(message = "Khóa của nhóm (groupKey) không được để trống")
    private String groupKey;

    @NotNull(message = "Layout hiển thị của nhóm không được để trống")
    private BannerGroupLayoutType displayLayout;

    @Valid // Rất quan trọng: Bật validation cho các đối tượng trong danh sách
    @NotNull(message = "Danh sách banner không được để trống")
    @Size(min = 1, message = "Phải có ít nhất một banner trong nhóm")
    private List<BannerItem> banners;

    // Lớp nội bộ để định nghĩa thông tin cho mỗi banner
    @Data
    public static class BannerItem {
        @NotBlank(message = "Tên banner không được để trống")
        private String bannerName;

        @NotBlank(message = "URL hình ảnh không được để trống")
        @URL(message = "URL hình ảnh không hợp lệ")
        private String imageUrl;

        @URL(message = "URL đích không hợp lệ")
        private String targetUrl;

        @NotNull(message = "Thứ tự hiển thị không được để trống")
        @Min(value = 0, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
        private Integer displayOrder;

        private boolean isActive = true;
    }
}
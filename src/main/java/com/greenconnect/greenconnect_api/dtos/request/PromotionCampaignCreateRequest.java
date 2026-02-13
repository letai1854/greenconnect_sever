package com.greenconnect.greenconnect_api.dtos.request;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.greenconnect.greenconnect_api.enums.CampaignType;
import com.greenconnect.greenconnect_api.enums.DiscountType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PromotionCampaignCreateRequest {

    // --- Thông tin chiến dịch ---
    @NotBlank(message = "Tên chiến dịch không được để trống")
    private String campaignName;
    
    private String description;
    
    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;
    
    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;
    
    private Boolean isActive = true;
    
    @NotBlank(message = "Slug không được để trống")
    private String slug;
    
    private String coverBannerUrl;
    
    private String urlViewAll;
    
    private CampaignType campaignType;
    
    private Integer displayOrder = 0;
    
    private java.math.BigDecimal discountPercentage; // % giảm giá chung cho campaign (0-100)

    // --- Danh sách sản phẩm được thêm vào ---
    @Valid // Quan trọng: Để kích hoạt validation cho các đối tượng trong list
    @NotEmpty(message = "Phải thêm ít nhất một sản phẩm vào chiến dịch")
    @JsonProperty("promotionProducts") // accept JSON with key 'promotionProducts'
    private List<ProductDiscountRequest> products;

    // Lớp nội bộ định nghĩa thông tin giảm giá cho mỗi sản phẩm
    @Data
    public static class ProductDiscountRequest {
        @NotNull(message = "Product ID không được để trống")
        private UUID productId;
        @NotNull(message = "Loại giảm giá không được để trống")
        private DiscountType discountType; // Sử dụng Enum để an toàn hơn
        @NotNull(message = "Giá trị giảm giá không được để trống")
        private BigDecimal discountValue;
    }
}
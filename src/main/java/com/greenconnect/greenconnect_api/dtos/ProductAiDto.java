package com.greenconnect.greenconnect_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO đơn giản để gửi cho Gemini AI làm context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAiDto {
    private UUID id;
    private String name;
    private String category;
    private String price;           // Range giá được tính từ variants
    private String description;
    
    // 🔥 NEW: Stock status để AI không gợi ý hết hàng
    private String stockStatus;     // "IN_STOCK", "OUT_OF_STOCK", "LOW_STOCK"
    private Integer totalStock;     // Tổng số lượng tồn kho
    private String unit;            // Đơn vị tính (VD: "kg", "gói", "hộp")
    
    // 🔥 NEW: Attributes quan trọng
    private String origin;          // Xuất xứ (VD: "Việt Nam", "Nhật Bản")
    private String weight;          // Trọng lượng (VD: "500g", "1kg")
    private String expiryInfo;      // Hạn sử dụng/Ngày hết hạn
    
    // 🔥 METADATA: Để AI hiểu sản phẩm mới/nổi bật/khuyến mãi/đánh giá cao
    private String createdAt;       // Thời gian tạo (để AI biết sản phẩm mới)
    private Boolean isFeatured;     // Sản phẩm nổi bật
    private Boolean isFlashSale;    // Đang khuyến mãi flash sale
    private Double averageRating;   // Đánh giá trung bình (0-5 sao)
    private Integer soldCount;      // Số lượng đã bán (để AI biết sản phẩm hot)
}

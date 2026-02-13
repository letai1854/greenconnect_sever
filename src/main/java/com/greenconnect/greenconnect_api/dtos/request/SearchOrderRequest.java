package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request tìm kiếm đơn hàng
 * 
 * <p><b>Tham số tìm kiếm:</b></p>
 * <ul>
 *   <li>keyword: Từ khóa tìm kiếm (orderCode, recipientName, recipientPhone, deliveryAddress)</li>
 *   <li>tab: Trạng thái đơn hàng (all, dang_cho, da_xac_nhan, dang_giao, da_giao, da_huy, tra_hang)</li>
 *   <li>page: Số trang (mặc định 0)</li>
 *   <li>size: Số lượng đơn hàng mỗi trang (mặc định 20)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchOrderRequest {
    
    @NotBlank(message = "Từ khóa tìm kiếm không được để trống")
    private String keyword;
    
    @NotBlank(message = "Tab không được để trống")
    @Pattern(regexp = "^(all|dang_cho|da_xac_nhan|dang_giao|da_giao|da_huy|tra_hang|yeu_cau_tra_hang|tra_hang_thanh_cong|tra_hang_that_bai|chua_thanh_toan|da_thanh_toan|that_bai)$", 
             message = "Tab không hợp lệ. Chỉ chấp nhận: all, dang_cho, da_xac_nhan, dang_giao, da_giao, da_huy, tra_hang, yeu_cau_tra_hang, tra_hang_thanh_cong, tra_hang_that_bai, chua_thanh_toan, da_thanh_toan, that_bai")
    private String tab;
    
    @Pattern(regexp = "^(all|today|this_week|this_month)$", 
             message = "DateFilter không hợp lệ. Chỉ chấp nhận: all, today, this_week, this_month")
    @Builder.Default
    private String dateFilter = "all";
    
    @Min(value = 0, message = "Số trang phải >= 0")
    @Builder.Default
    private int page = 0;
    
    @Min(value = 1, message = "Số lượng phải >= 1")
    @Builder.Default
    private int size = 20;
}

package com.greenconnect.greenconnect_api.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response từ GHTK API khi tính phí vận chuyển
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingFeeResponse {
    
    private Boolean success;
    
    private FeeDetail fee;
    
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeeDetail {
        private String name; // Vùng (ví dụ: area1)
        
        private Integer fee; // Phí vận chuyển
        
        @JsonProperty("insurance_fee")
        private Integer insuranceFee; // Phí bảo hiểm
        
        @JsonProperty("include_vat")
        private Integer includeVat; // VAT
        
        @JsonProperty("cost_id")
        private Integer costId;
        
        @JsonProperty("delivery_type")
        private String deliveryType;
        
        private Integer a;
        
        private String dt; // Loại giao hàng (local, national, etc.)
        
        @JsonProperty("extFees")
        private java.util.List<Object> extFees; // Phí mở rộng
        
        @JsonProperty("promotion_key")
        private String promotionKey;
        
        private Boolean delivery; // Có giao được không
        
        @JsonProperty("ship_fee_only")
        private Integer shipFeeOnly; // Phí ship thuần túy (không bao gồm các phí khác)
        
        private Double distance; // Khoảng cách (km)
        
        private FeeOptions options;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeeOptions {
        private String name;
        private String title;
        
        @JsonProperty("shipMoney")
        private Integer shipMoney;
        
        @JsonProperty("shipMoneyText")
        private String shipMoneyText; // Hiển thị dạng text (ví dụ: "33.500 đ")
        
        @JsonProperty("vatText")
        private String vatText;
        
        private String desc;
        private String coupon;
        
        @JsonProperty("maxUses")
        private Integer maxUses;
        
        @JsonProperty("maxDates")
        private Integer maxDates;
        
        @JsonProperty("maxDateString")
        private String maxDateString;
        
        private String content;
        
        @JsonProperty("activatedDate")
        private String activatedDate;
        
        @JsonProperty("couponTitle")
        private String couponTitle;
        
        private String discount;
        
        @JsonProperty("couponId")
        private Integer couponId;
    }
}

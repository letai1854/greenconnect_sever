package com.greenconnect.greenconnect_api.dtos.response.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipCustomersResponse {
    
    private Long totalVipCustomers;
    private List<VipCustomer> customers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VipCustomer {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private String avatar;
        private BigDecimal totalSpent;
        private Long orderCount;
        private Double averageRating;
        private LocalDateTime lastOrderDate;
        private LocalDateTime memberSince;
    }
}

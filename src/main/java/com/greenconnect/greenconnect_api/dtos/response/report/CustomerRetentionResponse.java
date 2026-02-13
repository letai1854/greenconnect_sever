package com.greenconnect.greenconnect_api.dtos.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRetentionResponse {
    
    private Double retentionRate;       // % khách quay lại
    private Double newCustomerRate;     // % khách mới
    private Long totalCustomers;        // Tổng khách trong khoảng thời gian
    private Long returningCustomers;    // Số khách quay lại
    private Long newCustomers;          // Số khách mới
}
